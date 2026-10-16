package com.nexora.flight.repository;

import com.nexora.flight.entity.FlightRoute;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlightRouteRepository extends JpaRepository<FlightRoute, Long> {
    @EntityGraph(attributePaths = {"origin", "destination"})
    List<FlightRoute> findByActiveTrue();
}
