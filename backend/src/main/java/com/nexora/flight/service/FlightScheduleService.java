package com.nexora.flight.service;

import com.nexora.flight.entity.Flight;
import com.nexora.flight.entity.FlightRoute;
import com.nexora.flight.entity.FlightSeat;
import com.nexora.flight.repository.FlightRepository;
import com.nexora.flight.repository.FlightRouteRepository;
import com.nexora.flight.repository.FlightSeatRepository;
import com.nexora.shared.config.PlatformSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeps a rolling schedule of demo flights: every active route gets one flight per day for the next
 * {@code nexora.flight-schedule-horizon-days} days. Idempotent (unique flight number + departure).
 */
@Service
public class FlightScheduleService {

    private static final Logger log = LoggerFactory.getLogger(FlightScheduleService.class);

    private final FlightRouteRepository routes;
    private final FlightRepository flights;
    private final FlightSeatRepository seats;
    private final PlatformSettings settings;
    private final Clock clock;

    public FlightScheduleService(FlightRouteRepository routes, FlightRepository flights, FlightSeatRepository seats,
                                 PlatformSettings settings, Clock clock) {
        this.routes = routes;
        this.flights = flights;
        this.seats = seats;
        this.settings = settings;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        ensureSchedule();
    }

    @Scheduled(cron = "0 15 0 * * *", zone = "UTC")
    public void daily() {
        ensureSchedule();
    }

    @Transactional
    public int ensureSchedule() {
        int created = 0;
        Instant now = clock.instant();
        for (FlightRoute route : routes.findByActiveTrue()) {
            LocalDate today = LocalDate.ofInstant(now, route.getOrigin().zone());
            for (int d = 0; d <= settings.flightScheduleHorizonDays(); d++) {
                Instant departure = today.plusDays(d).atTime(route.getDepartureLocal()).atZone(route.getOrigin().zone()).toInstant();
                if (departure.isBefore(now) || flights.existsByFlightNumberAndDepartureTime(route.getFlightNumber(), departure)) {
                    continue;
                }
                Flight flight = flights.save(new Flight(route, departure));
                List<FlightSeat> list = new ArrayList<>();
                for (int row = 1; row <= route.getSeatRows(); row++) {
                    for (int s = 0; s < route.getSeatsPerRow(); s++) {
                        list.add(new FlightSeat(flight, row + String.valueOf((char) ('A' + s))));
                    }
                }
                seats.saveAll(list);
                created++;
            }
        }
        if (created > 0) {
            log.info("Flight schedule: generated {} new demo flights", created);
        }
        return created;
    }
}
