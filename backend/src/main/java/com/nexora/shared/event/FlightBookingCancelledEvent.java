package com.nexora.shared.event;

import java.time.Instant;

public record FlightBookingCancelledEvent(Long bookingId, String reference, Long userId, String flightNumber,
                                          String reason, boolean expired, Instant occurredAt) implements DomainEvent {
    @Override public String module() { return "FLIGHT"; }
    @Override public String summary() {
        return "Брондау " + reference + (expired ? " мерзімі өтті: " : " болдырылмады: ") + flightNumber;
    }
}
