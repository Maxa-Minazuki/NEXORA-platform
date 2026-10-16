package com.nexora.shared.event;

import java.time.Instant;

public record FlightBookingConfirmedEvent(Long bookingId, String reference, Long userId, String flightNumber,
                                          String route, Instant departureTime, String confirmationNumber,
                                          Instant occurredAt) implements DomainEvent {
    @Override public String module() { return "FLIGHT"; }
    @Override public String summary() { return "Брондау " + reference + " расталды: " + flightNumber; }
}
