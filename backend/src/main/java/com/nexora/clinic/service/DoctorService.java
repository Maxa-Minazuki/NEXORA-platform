package com.nexora.clinic.service;

import com.nexora.clinic.dto.ClinicDtos.DoctorDto;
import com.nexora.clinic.dto.ClinicDtos.SpecialtyDto;
import com.nexora.clinic.repository.DoctorRepository;
import com.nexora.clinic.repository.SpecialtyRepository;
import com.nexora.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DoctorService {

    private final DoctorRepository doctors;
    private final SpecialtyRepository specialties;

    public DoctorService(DoctorRepository doctors, SpecialtyRepository specialties) {
        this.doctors = doctors;
        this.specialties = specialties;
    }

    @Transactional(readOnly = true)
    public List<DoctorDto> list(Long specialtyId) {
        return doctors.findByActiveTrueOrderByFullNameAsc().stream()
                .filter(d -> specialtyId == null || d.getSpecialty().getId().equals(specialtyId))
                .map(d -> {
                    d.getSchedules().size();
                    return ClinicMapper.toDto(d, true);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public DoctorDto get(Long id) {
        return ClinicMapper.toDto(doctors.findWithScheduleById(id).orElseThrow(() -> new NotFoundException("Doctor", id)), true);
    }

    @Transactional(readOnly = true)
    public List<SpecialtyDto> specialties() {
        return specialties.findAllByOrderByNameAsc().stream()
                .map(s -> new SpecialtyDto(s.getId(), s.getName(), s.getDescription())).toList();
    }
}
