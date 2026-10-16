package com.nexora.flight.mediator;

import com.nexora.flight.entity.Flight;
import com.nexora.flight.repository.FlightRepository;
import com.nexora.shared.exception.BusinessRuleException;
import com.nexora.shared.exception.ConflictException;
import com.nexora.shared.exception.NotFoundException;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Colleague: locks the flight row and verifies it is bookable for the requested party size. */
@Component
public class FlightAvailabilityChecker extends BookingColleague {

    static final Duration MIN_TIME_BEFORE_DEPARTURE = Duration.ofHours(1);

    private final FlightRepository flights;

    public FlightAvailabilityChecker(FlightRepository flights) {
        this.flights = flights;
    }

    @Override public String role() { return "FlightAvailabilityChecker"; }

    void verify(BookingContext ctx) {
        Flight flight = flights.lockById(ctx.flightId).orElseThrow(() -> new NotFoundException("Flight", ctx.flightId));
        if (!"SCHEDULED".equals(flight.getStatus())) {
            throw new BusinessRuleException("FLIGHT_NOT_BOOKABLE", flight.getFlightNumber() + " рейсіне брондау қолжетімсіз");
        }
        if (flight.getDepartureTime().isBefore(ctx.now.plus(MIN_TIME_BEFORE_DEPARTURE))) {
            throw new BusinessRuleException("BOOKING_CLOSED", "Онлайн брондау ұшуға 1 сағат қалғанда жабылады");
        }
        if (flight.getAvailableSeats() < ctx.passengers.size()) {
            throw new ConflictException("NOT_ENOUGH_SEATS", flight.getFlightNumber() + " рейсінде тек "
                    + flight.getAvailableSeats() + " бос орын қалды");
        }
        ctx.flight = flight;
        ctx.log(role() + ": " + flight.getFlightNumber() + " рейсі құлыпталды, бос орын: " + flight.getAvailableSeats());
        report(BookingSignal.FLIGHT_VERIFIED, ctx);
    }
}
