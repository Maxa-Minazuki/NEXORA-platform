package com.nexora.clinic.service;

import com.nexora.clinic.dto.ClinicDtos.AppointmentDto;
import com.nexora.clinic.entity.Appointment;
import com.nexora.clinic.entity.AppointmentStatus;
import com.nexora.clinic.repository.AppointmentRepository;
import com.nexora.shared.exception.NotFoundException;
import com.nexora.shared.security.AuthenticatedUser;
import com.nexora.shared.web.PageRequests;
import com.nexora.shared.web.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Service
public class AppointmentQueryService {

    private final AppointmentRepository appointments;
    private final ClinicAccessPolicy access;

    public AppointmentQueryService(AppointmentRepository appointments, ClinicAccessPolicy access) {
        this.appointments = appointments;
        this.access = access;
    }

    public record AppointmentFilter(Long doctorId, Long patientId, Long specialtyId, AppointmentStatus status,
                                    Instant from, Instant to, String q) {
    }

    @Transactional(readOnly = true)
    public PageResponse<AppointmentDto> search(AppointmentFilter f, AuthenticatedUser user, int page, int size, String sort) {
        Long patientId = access.restrictedPatientId(user).orElse(f.patientId());
        Long doctorId = access.restrictedDoctorId(user).orElse(f.doctorId());
        var pageable = PageRequests.of(page, size, sort, Set.of("startTime", "status", "createdAt"), "startTime");
        String q = f.q() == null || f.q().isBlank() ? null : f.q().trim();
        return PageResponse.of(appointments.search(doctorId, patientId, f.specialtyId(), f.status(), f.from(), f.to(), q,
                pageable), ClinicMapper::toDto);
    }

    @Transactional(readOnly = true)
    public AppointmentDto get(Long id, AuthenticatedUser user) {
        Appointment a = appointments.findWithDetailsById(id);
        if (a == null) {
            throw new NotFoundException("Appointment", id);
        }
        access.restrictedPatientId(user).ifPresent(pid -> {
            if (!pid.equals(a.getPatient().getId())) {
                throw new NotFoundException("Appointment", id);
            }
        });
        access.restrictedDoctorId(user).ifPresent(did -> {
            if (!did.equals(a.getDoctor().getId())) {
                throw new NotFoundException("Appointment", id);
            }
        });
        a.getDoctor().getSpecialty().getName();
        return ClinicMapper.toDto(a);
    }
}
