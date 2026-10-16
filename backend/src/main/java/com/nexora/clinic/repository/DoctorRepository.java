package com.nexora.clinic.repository;

import com.nexora.clinic.entity.Doctor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    /**
     * Locks the doctor row for the duration of the booking transaction. All bookings for one doctor
     * are serialized through this lock, so two concurrent requests cannot both pass the overlap check.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Doctor d where d.id = :id")
    Optional<Doctor> lockById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"specialty"})
    List<Doctor> findByActiveTrueOrderByFullNameAsc();

    @EntityGraph(attributePaths = {"specialty", "schedules"})
    Optional<Doctor> findWithScheduleById(Long id);

    Optional<Doctor> findByUserId(Long userId);

    long countByActiveTrue();
}
