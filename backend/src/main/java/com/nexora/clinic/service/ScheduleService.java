package com.nexora.clinic.service;

import com.nexora.clinic.dto.ClinicDtos.DaySlots;
import com.nexora.clinic.dto.ClinicDtos.SlotDto;
import com.nexora.clinic.entity.Appointment;
import com.nexora.clinic.entity.AppointmentStatus;
import com.nexora.clinic.entity.Doctor;
import com.nexora.clinic.entity.DoctorSchedule;
import com.nexora.clinic.repository.AppointmentRepository;
import com.nexora.clinic.repository.DoctorRepository;
import com.nexora.shared.config.PlatformSettings;
import com.nexora.shared.exception.BusinessRuleException;
import com.nexora.shared.exception.ConflictException;
import com.nexora.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Working hours, slot grid and conflict detection for doctors' calendars. */
@Service
public class ScheduleService {

    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;
    private final PlatformSettings settings;
    private final Clock clock;

    public ScheduleService(DoctorRepository doctors, AppointmentRepository appointments, PlatformSettings settings,
                           Clock clock) {
        this.doctors = doctors;
        this.appointments = appointments;
        this.settings = settings;
        this.clock = clock;
    }

    public Duration slotLength() {
        return Duration.ofMinutes(settings.appointmentSlotMinutes());
    }

    /** Validates that {@code start} is in the future, inside working hours and on the slot grid. */
    public void validateWithinWorkingHours(Doctor doctor, Instant start) {
        if (!start.isAfter(clock.instant())) {
            throw new BusinessRuleException("SLOT_IN_PAST", "Қабылдау уақыты болашақта болуы керек");
        }
        ZoneId zone = settings.businessZone();
        ZonedDateTime local = start.atZone(zone);
        DoctorSchedule day = scheduleFor(doctor, local.toLocalDate()).orElseThrow(() ->
                new BusinessRuleException("NOT_WORKING_DAY", doctor.getFullName() + " " + local.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, com.nexora.shared.i18n.Kk.LOCALE)
                        + " күні жұмыс істемейді"));
        LocalTime t = local.toLocalTime();
        LocalTime end = t.plus(slotLength());
        if (t.isBefore(day.getStartTime()) || end.isAfter(day.getEndTime()) || end.isBefore(t)) {
            throw new BusinessRuleException("OUTSIDE_WORKING_HOURS", "Бұл күнгі жұмыс уақыты: "
                    + day.getStartTime() + "-" + day.getEndTime() + " (" + zone + ")");
        }
        long minutesFromStart = Duration.between(day.getStartTime(), t).toMinutes();
        if (minutesFromStart % settings.appointmentSlotMinutes() != 0 || local.getSecond() != 0 || local.getNano() != 0) {
            throw new BusinessRuleException("SLOT_MISALIGNED", "Қабылдау " + day.getStartTime() + " бастап әр "
                    + settings.appointmentSlotMinutes() + " минут сайын басталады");
        }
    }

    /** Must be called while holding the doctor lock (see {@link DoctorRepository#lockById}). */
    public void ensureNoConflicts(Doctor doctor, Long patientId, Instant start, Instant end, Long excludeAppointmentId) {
        if (appointments.countDoctorOverlaps(doctor.getId(), start, end, AppointmentStatus.activeStatuses(),
                excludeAppointmentId) > 0) {
            throw new ConflictException("SLOT_TAKEN", doctor.getFullName() + " дәрігердің бұл уақытта басқа қабылдауы бар");
        }
        if (appointments.countPatientOverlaps(patientId, start, end, AppointmentStatus.activeStatuses(),
                excludeAppointmentId) > 0) {
            throw new ConflictException("PATIENT_BUSY", "Пациенттің бұл уақытта басқа қабылдауы бар");
        }
    }

    @Transactional(readOnly = true)
    public DaySlots slots(Long doctorId, LocalDate date) {
        Doctor doctor = doctors.findWithScheduleById(doctorId).orElseThrow(() -> new NotFoundException("Doctor", doctorId));
        ZoneId zone = settings.businessZone();
        Optional<DoctorSchedule> day = scheduleFor(doctor, date);
        if (day.isEmpty()) {
            return new DaySlots(doctorId, date, zone.getId(), settings.appointmentSlotMinutes(), false, List.of());
        }
        Instant dayStart = date.atStartOfDay(zone).toInstant();
        List<Appointment> booked = appointments.findDoctorDay(doctorId, dayStart, date.plusDays(1).atStartOfDay(zone).toInstant(),
                AppointmentStatus.activeStatuses());
        List<SlotDto> result = new ArrayList<>();
        Instant now = clock.instant();
        for (LocalTime t = day.get().getStartTime(); !t.plus(slotLength()).isAfter(day.get().getEndTime());
             t = t.plus(slotLength())) {
            Instant s = date.atTime(t).atZone(zone).toInstant();
            Instant e = s.plus(slotLength());
            boolean free = s.isAfter(now) && booked.stream()
                    .noneMatch(a -> a.getStartTime().isBefore(e) && a.getEndTime().isAfter(s));
            result.add(new SlotDto(s, e, free));
            if (t.plus(slotLength()).isBefore(t)) {
                break; // wrapped past midnight
            }
        }
        return new DaySlots(doctorId, date, zone.getId(), settings.appointmentSlotMinutes(), true, result);
    }

    private static Optional<DoctorSchedule> scheduleFor(Doctor doctor, LocalDate date) {
        return doctor.getSchedules().stream().filter(s -> s.getDayOfWeek() == date.getDayOfWeek()).findFirst();
    }
}
