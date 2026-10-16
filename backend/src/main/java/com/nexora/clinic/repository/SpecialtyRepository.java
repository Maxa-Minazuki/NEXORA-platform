package com.nexora.clinic.repository;

import com.nexora.clinic.entity.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {
    List<Specialty> findAllByOrderByNameAsc();
}
