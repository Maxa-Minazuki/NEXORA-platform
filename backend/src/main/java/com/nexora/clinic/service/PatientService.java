package com.nexora.clinic.service;

import com.nexora.clinic.dto.ClinicDtos.PatientDto;
import com.nexora.clinic.dto.ClinicDtos.PatientRequest;
import com.nexora.clinic.entity.Patient;
import com.nexora.clinic.repository.PatientRepository;
import com.nexora.shared.exception.NotFoundException;
import com.nexora.shared.web.PageRequests;
import com.nexora.shared.web.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
public class PatientService {

    private final PatientRepository patients;

    public PatientService(PatientRepository patients) {
        this.patients = patients;
    }

    @Transactional(readOnly = true)
    public PageResponse<PatientDto> search(String q, int page, int size, String sort) {
        var pageable = PageRequests.of(page, size, sort, Set.of("lastName", "firstName", "dateOfBirth", "createdAt"), "lastName");
        String query = q == null || q.isBlank() ? null : q.trim();
        return PageResponse.of(patients.search(query, pageable), ClinicMapper::toDto);
    }

    @Transactional(readOnly = true)
    public PatientDto get(Long id) {
        return ClinicMapper.toDto(patients.findById(id).orElseThrow(() -> new NotFoundException("Patient", id)));
    }

    @Transactional(readOnly = true)
    public Optional<PatientDto> forUser(Long userId) {
        return patients.findByUserId(userId).map(ClinicMapper::toDto);
    }

    @Transactional
    public PatientDto create(PatientRequest r) {
        Patient p = new Patient(r.firstName().trim(), r.lastName().trim(), r.dateOfBirth(), r.gender());
        p.update(r.firstName().trim(), r.lastName().trim(), r.dateOfBirth(), r.gender(), r.phone(), r.email(), r.notes());
        return ClinicMapper.toDto(patients.save(p));
    }

    @Transactional
    public PatientDto update(Long id, PatientRequest r) {
        Patient p = patients.findById(id).orElseThrow(() -> new NotFoundException("Patient", id));
        p.update(r.firstName().trim(), r.lastName().trim(), r.dateOfBirth(), r.gender(), r.phone(), r.email(), r.notes());
        return ClinicMapper.toDto(p);
    }
}
