package com.nexora.flight.repository;

import com.nexora.flight.entity.Airport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AirportRepository extends JpaRepository<Airport, Long> {
    Optional<Airport> findByCodeIgnoreCase(String code);
    List<Airport> findAllByOrderByCityAsc();
}
