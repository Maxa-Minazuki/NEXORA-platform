package com.nexora.flight.state;

import com.nexora.flight.entity.BookingStatus;
import com.nexora.flight.entity.FlightBooking;
import com.nexora.shared.exception.InvalidStateTransitionException;

import java.time.Instant;
import java.util.Set;

/**
 * <h2>Design pattern: State</h2>
 *
 * Each lifecycle state of a {@link FlightBooking} is an object that decides which operations are
 * legal and what the next state is. By default every operation is rejected; concrete states override
 * only what they allow. Callers never assign a status directly - the API cannot send "status=CONFIRMED",
 * it can only ask for the {@code confirm} operation, and the current state decides.
 */
public interface BookingState {

    BookingStatus status();

    /** Names of the operations a client may offer in this state (drives the UI buttons). */
    Set<String> allowedActions();

    default void confirm(FlightBooking booking, Instant now) {
        throw new InvalidStateTransitionException("booking", status().name(), "confirm");
    }

    default void cancel(FlightBooking booking, String reason, Instant now) {
        throw new InvalidStateTransitionException("booking", status().name(), "cancel");
    }

    default void expire(FlightBooking booking, Instant now) {
        throw new InvalidStateTransitionException("booking", status().name(), "expire");
    }

    /** True when seats held/booked by the booking must be released after leaving this state. */
    default boolean holdsSeats() {
        return false;
    }
}
