package com.nexora.clinic.repository;

import com.nexora.clinic.entity.Appointment;
import com.nexora.clinic.entity.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /** Half-open interval overlap: [start, end) intersects [s, e) iff start < e and end > s. */
    @Query("""
            select count(a) from Appointment a
            where a.doctor.id = :doctorId and a.status in :statuses
              and a.startTime < :end and a.endTime > :start
              and (:excludeId is null or a.id <> :excludeId)
            """)
    long countDoctorOverlaps(@Param("doctorId") Long doctorId, @Param("start") Instant start, @Param("end") Instant end,
                             @Param("statuses") Collection<AppointmentStatus> statuses, @Param("excludeId") Long excludeId);

    @Query("""
            select count(a) from Appointment a
            where a.patient.id = :patientId and a.status in :statuses
              and a.startTime < :end and a.endTime > :start
              and (:excludeId is null or a.id <> :excludeId)
            """)
    long countPatientOverlaps(@Param("patientId") Long patientId, @Param("start") Instant start, @Param("end") Instant end,
                              @Param("statuses") Collection<AppointmentStatus> statuses, @Param("excludeId") Long excludeId);

    @EntityGraph(attributePaths = {"patient", "doctor", "doctor.specialty"})
    @Query("""
            select a from Appointment a
            where (:doctorId is null or a.doctor.id = :doctorId)
              and (:patientId is null or a.patient.id = :patientId)
              and (:specialtyId is null or a.doctor.specialty.id = :specialtyId)
              and (:status is null or a.status = :status)
              and (:from is null or a.startTime >= :from)
              and (:to is null or a.startTime < :to)
              and (:q is null or lower(concat(a.patient.firstName, ' ', a.patient.lastName)) like lower(concat('%', :q, '%')))
            """)
    Page<Appointment> search(@Param("doctorId") Long doctorId, @Param("patientId") Long patientId,
                             @Param("specialtyId") Long specialtyId, @Param("status") AppointmentStatus status,
                             @Param("from") Instant from, @Param("to") Instant to, @Param("q") String q,
                             Pageable pageable);

    @Query("""
            select a from Appointment a
            where a.doctor.id = :doctorId and a.status in :statuses and a.startTime >= :from and a.startTime < :to
            """)
    List<Appointment> findDoctorDay(@Param("doctorId") Long doctorId, @Param("from") Instant from,
                                    @Param("to") Instant to, @Param("statuses") Collection<AppointmentStatus> statuses);

    @EntityGraph(attributePaths = {"patient", "doctor"})
    Appointment findWithDetailsById(Long id);

    long countByStartTimeGreaterThanEqualAndStartTimeLessThan(Instant from, Instant to);

    long countByStatusInAndStartTimeGreaterThanEqual(Collection<AppointmentStatus> statuses, Instant from);

    @EntityGraph(attributePaths = {"patient", "doctor", "doctor.specialty"})
    List<Appointment> findTop6ByStatusInAndStartTimeGreaterThanEqualOrderByStartTimeAsc(
            Collection<AppointmentStatus> statuses, Instant from);

    @Query("select a from Appointment a where a.startTime >= :from and a.startTime < :to")
    List<Appointment> findStartingBetween(@Param("from") Instant from, @Param("to") Instant to);
}
