package com.nexora.clinic.repository;

import com.nexora.clinic.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    @Query("""
            select p from Patient p
            where :q is null
               or lower(concat(p.firstName, ' ', p.lastName)) like lower(concat('%', :q, '%'))
               or lower(p.email) like lower(concat('%', :q, '%'))
               or p.phone like concat('%', :q, '%')
            """)
    Page<Patient> search(@Param("q") String q, Pageable pageable);

    Optional<Patient> findByUserId(Long userId);
}
