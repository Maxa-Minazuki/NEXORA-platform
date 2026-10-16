package com.nexora.flight.state;

import com.nexora.flight.entity.BookingStatus;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Terminal: every operation is rejected by the interface defaults. */
@Component
public class CancelledBookingState implements BookingState {

    @Override public BookingStatus status() { return BookingStatus.CANCELLED; }

    @Override public Set<String> allowedActions() { return Set.of(); }
}
