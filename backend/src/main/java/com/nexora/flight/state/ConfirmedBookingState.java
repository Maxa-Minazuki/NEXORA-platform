package com.nexora.flight.state;

import com.nexora.flight.entity.BookingStatus;
import com.nexora.flight.entity.FlightBooking;
import com.nexora.shared.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/** Confirmed: can only be cancelled, and not later than 2 hours before departure. */
@Component
public class ConfirmedBookingState implements BookingState {

    static final Duration CANCELLATION_CUTOFF = Duration.ofHours(2);

    @Override public BookingStatus status() { return BookingStatus.CONFIRMED; }

    @Override public Set<String> allowedActions() { return Set.of("cancel"); }

    @Override public boolean holdsSeats() { return true; }

    @Override
    public void cancel(FlightBooking booking, String reason, Instant now) {
        if (now.isAfter(booking.getFlight().getDepartureTime().minus(CANCELLATION_CUTOFF))) {
            throw new BusinessRuleException("CANCELLATION_CLOSED", "Расталған брондауды ұшуға "
                    + CANCELLATION_CUTOFF.toHours() + " сағат қалғанға дейін ғана болдырмауға болады");
        }
        booking.moveTo(BookingStatus.CANCELLED, now, reason);
    }
}
