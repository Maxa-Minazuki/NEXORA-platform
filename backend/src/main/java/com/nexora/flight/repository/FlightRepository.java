package com.nexora.flight.repository;

import com.nexora.flight.entity.Flight;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FlightRepository extends JpaRepository<Flight, Long> {

    @EntityGraph(attributePaths = {"origin", "destination"})
    @Query("""
            select f from Flight f
            where f.origin.id = :originId and f.destination.id = :destinationId
              and f.departureTime >= :from and f.departureTime < :to
              and f.departureTime > :notBefore
              and f.status = 'SCHEDULED' and f.availableSeats >= :passengers
            """)
    List<Flight> search(@Param("originId") Long originId, @Param("destinationId") Long destinationId,
                        @Param("from") Instant from, @Param("to") Instant to, @Param("notBefore") Instant notBefore,
                        @Param("passengers") int passengers);

    /** Serializes seat allocation per flight: SELECT ... FOR UPDATE on the flight row. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Flight f where f.id = :id")
    Optional<Flight> lockById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"origin", "destination"})
    Optional<Flight> findDetailedById(Long id);

    boolean existsByFlightNumberAndDepartureTime(String flightNumber, Instant departureTime);

    @Query("select count(f) from Flight f where f.status = 'SCHEDULED' and f.departureTime > :now and f.availableSeats > 0")
    long countBookable(@Param("now") Instant now);
}
