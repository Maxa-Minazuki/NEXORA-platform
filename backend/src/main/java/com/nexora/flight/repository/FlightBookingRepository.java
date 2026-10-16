package com.nexora.flight.repository;

import com.nexora.flight.entity.BookingStatus;
import com.nexora.flight.entity.FlightBooking;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FlightBookingRepository extends JpaRepository<FlightBooking, Long> {

    boolean existsByReference(String reference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from FlightBooking b where b.id = :id")
    Optional<FlightBooking> lockById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"flight", "flight.origin", "flight.destination"})
    @Query("""
            select b from FlightBooking b
            where (:userId is null or b.userId = :userId)
              and (:status is null or b.status = :status)
              and (:q is null or lower(b.reference) like lower(concat('%', :q, '%'))
                   or lower(b.flight.flightNumber) like lower(concat('%', :q, '%')))
            """)
    Page<FlightBooking> search(@Param("userId") Long userId, @Param("status") BookingStatus status,
                               @Param("q") String q, Pageable pageable);

    @Query("select b.id from FlightBooking b where b.status = 'PENDING' and b.expiresAt < :now")
    List<Long> findExpiredPendingIds(@Param("now") Instant now);

    long countByStatus(BookingStatus status);

    @Query("select b.status, count(b) from FlightBooking b group by b.status")
    List<Object[]> countByStatusGrouped();
}
