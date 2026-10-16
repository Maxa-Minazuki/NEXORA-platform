package com.nexora.clinic.facade;

import com.nexora.clinic.dto.ClinicDtos.AppointmentDto;
import com.nexora.clinic.dto.ClinicDtos.BookAppointmentRequest;
import com.nexora.clinic.dto.ClinicDtos.StatusChangeRequest;
import com.nexora.clinic.entity.Appointment;
import com.nexora.clinic.entity.AppointmentStatus;
import com.nexora.clinic.entity.Doctor;
import com.nexora.clinic.entity.Patient;
import com.nexora.clinic.repository.AppointmentRepository;
import com.nexora.clinic.repository.DoctorRepository;
import com.nexora.clinic.repository.PatientRepository;
import com.nexora.clinic.service.ClinicAccessPolicy;
import com.nexora.clinic.service.ClinicMapper;
import com.nexora.clinic.service.ScheduleService;
import com.nexora.shared.event.AppointmentBookedEvent;
import com.nexora.shared.event.AppointmentCancelledEvent;
import com.nexora.shared.event.AppointmentRescheduledEvent;
import com.nexora.shared.exception.BusinessRuleException;
import com.nexora.shared.exception.NotFoundException;
import com.nexora.shared.security.AuthenticatedUser;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * <h2>Design pattern: Facade</h2>
 *
 * One simple entry point ({@code book / reschedule / cancel / changeStatus}) in front of the clinic
 * subsystem: access policy, patient and doctor lookup, doctor row locking, working-hours validation,
 * overlap detection, persistence and Observer event publication. The controller makes one call
 * instead of orchestrating six collaborators, and the order of the steps (lock before checking for
 * overlaps!) is encoded in exactly one place.
 */
@Service
public class PatientAppointmentFacade {

    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;
    private final ScheduleService schedule;
    private final ClinicAccessPolicy access;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public PatientAppointmentFacade(PatientRepository patients, DoctorRepository doctors,
                                    AppointmentRepository appointments, ScheduleService schedule,
                                    ClinicAccessPolicy access, ApplicationEventPublisher events, Clock clock) {
        this.patients = patients;
        this.doctors = doctors;
        this.appointments = appointments;
        this.schedule = schedule;
        this.access = access;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public AppointmentDto book(BookAppointmentRequest request, AuthenticatedUser actor) {
        Patient patient = patients.findById(request.patientId())
                .orElseThrow(() -> new NotFoundException("Patient", request.patientId()));
        access.checkCanBookFor(actor, patient);

        Doctor doctor = doctors.lockById(request.doctorId())
                .orElseThrow(() -> new NotFoundException("Doctor", request.doctorId()));
        if (!doctor.isActive()) {
            throw new BusinessRuleException("DOCTOR_INACTIVE", "Бұл дәрігер қазір қабылдау жүргізбейді");
        }
        Instant start = request.startTime();
        Instant end = start.plus(schedule.slotLength());
        schedule.validateWithinWorkingHours(doctor, start);
        schedule.ensureNoConflicts(doctor, patient.getId(), start, end, null);

        Appointment saved = appointments.save(new Appointment(patient, doctor, start, end, request.reason().trim(), actor.id()));
        events.publishEvent(new AppointmentBookedEvent(saved.getId(), patient.fullName(), patient.getUserId(),
                doctor.getFullName(), doctor.getUserId(), start, actor.id(), clock.instant()));
        return ClinicMapper.toDto(saved);
    }

    @Transactional
    public AppointmentDto reschedule(Long appointmentId, Instant newStart, AuthenticatedUser actor) {
        Appointment appointment = load(appointmentId);
        access.checkCanModify(actor, appointment);
        Doctor doctor = doctors.lockById(appointment.getDoctor().getId()).orElseThrow();
        Instant previous = appointment.getStartTime();
        Instant end = newStart.plus(schedule.slotLength());
        schedule.validateWithinWorkingHours(doctor, newStart);
        schedule.ensureNoConflicts(doctor, appointment.getPatient().getId(), newStart, end, appointment.getId());
        appointment.reschedule(newStart, end);
        events.publishEvent(new AppointmentRescheduledEvent(appointment.getId(), appointment.getPatient().fullName(),
                appointment.getPatient().getUserId(), doctor.getFullName(), doctor.getUserId(), previous, newStart,
                actor.id(), clock.instant()));
        return ClinicMapper.toDto(appointment);
    }

    @Transactional
    public AppointmentDto cancel(Long appointmentId, String reason, AuthenticatedUser actor) {
        Appointment appointment = load(appointmentId);
        access.checkCanModify(actor, appointment);
        appointment.cancel(reason.trim());
        events.publishEvent(new AppointmentCancelledEvent(appointment.getId(), appointment.getPatient().fullName(),
                appointment.getPatient().getUserId(), appointment.getDoctor().getFullName(),
                appointment.getDoctor().getUserId(), appointment.getStartTime(), reason, actor.id(), clock.instant()));
        return ClinicMapper.toDto(appointment);
    }

    @Transactional
    public AppointmentDto changeStatus(Long appointmentId, StatusChangeRequest request, AuthenticatedUser actor) {
        if (request.status() == AppointmentStatus.CANCELLED || request.status() == AppointmentStatus.SCHEDULED) {
            throw new BusinessRuleException("USE_DEDICATED_ACTION", "Бұл үшін «Болдырмау» немесе «Ауыстыру» әрекетін қолданыңыз");
        }
        Appointment appointment = load(appointmentId);
        access.checkCanSetOutcome(actor, appointment);
        if ((request.status() == AppointmentStatus.COMPLETED || request.status() == AppointmentStatus.NO_SHOW)
                && appointment.getStartTime().isAfter(clock.instant())) {
            throw new BusinessRuleException("NOT_STARTED", "Қабылдауды тек басталғаннан кейін ғана аяқталған немесе «келмеді» деп белгілеуге болады");
        }
        appointment.transitionTo(request.status());
        if (request.notes() != null && !request.notes().isBlank()) {
            appointment.setNotes(request.notes().trim());
        }
        return ClinicMapper.toDto(appointment);
    }

    private Appointment load(Long id) {
        Appointment a = appointments.findWithDetailsById(id);
        if (a == null) {
            throw new NotFoundException("Appointment", id);
        }
        return a;
    }
}
