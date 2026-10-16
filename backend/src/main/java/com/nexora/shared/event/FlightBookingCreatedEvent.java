package com.nexora.shared.event;

import java.time.Instant;

public record FlightBookingCreatedEvent(Long bookingId, String reference, Long userId, String flightNumber,
                                        String route, Instant expiresAt, Instant occurredAt) implements DomainEvent {
    @Override public String module() { return "FLIGHT"; }
    @Override public String summary() { return "Брондау " + reference + " жасалды: " + flightNumber + " (" + route + ")"; }
}
