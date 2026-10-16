package com.nexora.flight.repository;

import com.nexora.flight.entity.FlightSeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface FlightSeatRepository extends JpaRepository<FlightSeat, Long> {

    List<FlightSeat> findByFlightIdOrderByIdAsc(Long flightId);

    List<FlightSeat> findByFlightIdAndStatusOrderByIdAsc(Long flightId, FlightSeat.Status status);

    List<FlightSeat> findByFlightIdAndSeatNumberIn(Long flightId, Collection<String> seatNumbers);

    List<FlightSeat> findByBookingId(Long bookingId);
}
