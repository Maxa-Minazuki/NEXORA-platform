package com.nexora.clinic.service;

import com.nexora.clinic.dto.ClinicDtos.AppointmentDto;
import com.nexora.clinic.dto.ClinicDtos.DoctorDto;
import com.nexora.clinic.dto.ClinicDtos.PatientDto;
import com.nexora.clinic.dto.ClinicDtos.ScheduleDto;
import com.nexora.clinic.entity.Appointment;
import com.nexora.clinic.entity.Doctor;
import com.nexora.clinic.entity.Patient;

import java.util.List;

public final class ClinicMapper {

    private ClinicMapper() {
    }

    public static PatientDto toDto(Patient p) {
        return new PatientDto(p.getId(), p.getFirstName(), p.getLastName(), p.fullName(), p.getDateOfBirth(),
                p.getGender(), p.getPhone(), p.getEmail(), p.getNotes(), p.getUserId() != null, p.getCreatedAt());
    }

    public static DoctorDto toDto(Doctor d, boolean withSchedule) {
        List<ScheduleDto> schedule = withSchedule
                ? d.getSchedules().stream().map(s -> new ScheduleDto(s.getDayOfWeek(), s.getStartTime(), s.getEndTime())).toList()
                : List.of();
        return new DoctorDto(d.getId(), d.getFullName(), d.getSpecialty().getId(), d.getSpecialty().getName(),
                d.getEmail(), d.getPhone(), d.getRoom(), d.getBio(), schedule);
    }

    public static AppointmentDto toDto(Appointment a) {
        return new AppointmentDto(a.getId(), a.getPatient().getId(), a.getPatient().fullName(), a.getDoctor().getId(),
                a.getDoctor().getFullName(), a.getDoctor().getSpecialty().getName(), a.getDoctor().getRoom(),
                a.getStartTime(), a.getEndTime(), a.getStatus(), a.getReason(), a.getNotes(), a.getCancelReason(),
                a.getStatus().allowedNext().stream().sorted().toList(), a.getCreatedAt());
    }
}
