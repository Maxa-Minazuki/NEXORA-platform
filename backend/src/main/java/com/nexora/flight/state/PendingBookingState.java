package com.nexora.flight.state;

import com.nexora.flight.entity.BookingStatus;
import com.nexora.flight.entity.FlightBooking;
import com.nexora.shared.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

/** Seats are held for a limited time; the booking may be confirmed, cancelled or expire. */
@Component
public class PendingBookingState implements BookingState {

    @Override public BookingStatus status() { return BookingStatus.PENDING; }

    @Override public Set<String> allowedActions() { return Set.of("confirm", "cancel"); }

    @Override public boolean holdsSeats() { return true; }

    @Override
    public void confirm(FlightBooking booking, Instant now) {
        if (now.isAfter(booking.getExpiresAt())) {
            throw new BusinessRuleException("HOLD_EXPIRED", booking.getReference() + " брондауы бойынша орындарды ұстау мерзімі өтті. "
                    + "Жаңа брондау жасау үшін қайта іздеңіз.");
        }
        booking.moveTo(BookingStatus.CONFIRMED, now, null);
    }

    @Override
    public void cancel(FlightBooking booking, String reason, Instant now) {
        booking.moveTo(BookingStatus.CANCELLED, now, reason);
    }

    @Override
    public void expire(FlightBooking booking, Instant now) {
        booking.moveTo(BookingStatus.EXPIRED, now, "Орындарды ұстау мерзімі растаудан бұрын аяқталды");
    }
}
