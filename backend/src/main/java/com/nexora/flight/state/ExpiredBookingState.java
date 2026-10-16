package com.nexora.flight.state;

import com.nexora.flight.entity.BookingStatus;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Terminal: the hold lapsed; seats were returned to the pool. */
@Component
public class ExpiredBookingState implements BookingState {

    @Override public BookingStatus status() { return BookingStatus.EXPIRED; }

    @Override public Set<String> allowedActions() { return Set.of(); }
}
