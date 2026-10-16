package com.nexora.flight.state;

import com.nexora.flight.entity.BookingStatus;
import com.nexora.flight.entity.FlightBooking;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Restores the State object from the persisted status (so state survives restarts). */
@Component
public class BookingStateRegistry {

    private final Map<BookingStatus, BookingState> states = new EnumMap<>(BookingStatus.class);

    public BookingStateRegistry(List<BookingState> all) {
        all.forEach(s -> states.put(s.status(), s));
        for (BookingStatus status : BookingStatus.values()) {
            if (!states.containsKey(status)) {
                throw new IllegalStateException("No BookingState implementation for " + status);
            }
        }
    }

    public BookingState of(FlightBooking booking) {
        return states.get(booking.getStatus());
    }

    public BookingState of(BookingStatus status) {
        return states.get(status);
    }
}
