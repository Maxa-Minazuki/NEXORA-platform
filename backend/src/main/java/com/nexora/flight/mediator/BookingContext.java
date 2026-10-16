package com.nexora.flight.mediator;

import com.nexora.flight.dto.FlightDtos.PassengerRequest;
import com.nexora.flight.entity.BookingDocument;
import com.nexora.flight.entity.Flight;
import com.nexora.flight.entity.FlightBooking;
import com.nexora.flight.entity.FlightSeat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Mutable, per-request working data of one booking scenario. Created for each request and discarded
 * afterwards - never stored in a singleton.
 */
public final class BookingContext {

    final Long flightId;
    final Long userId;
    final List<PassengerRequest> passengers;
    final String contactEmail;
    final String reason;
    final Instant now;

    Flight flight;
    List<FlightSeat> seats = List.of();
    FlightBooking booking;
    BookingDocument document;
    boolean expired;
    private final List<String> log = new ArrayList<>();

    private BookingContext(Long flightId, Long userId, List<PassengerRequest> passengers, String contactEmail,
                           String reason, Instant now) {
        this.flightId = flightId;
        this.userId = userId;
        this.passengers = passengers;
        this.contactEmail = contactEmail;
        this.reason = reason;
        this.now = now;
    }

    public static BookingContext forCreate(Long flightId, Long userId, List<PassengerRequest> passengers,
                                           String contactEmail, Instant now) {
        return new BookingContext(flightId, userId, List.copyOf(passengers), contactEmail, null, now);
    }

    public static BookingContext forExisting(FlightBooking booking, String reason, Instant now) {
        BookingContext ctx = new BookingContext(booking.getFlight().getId(), booking.getUserId(), List.of(),
                booking.getContactEmail(), reason, now);
        ctx.booking = booking;
        ctx.flight = booking.getFlight();
        return ctx;
    }

    void log(String line) {
        log.add(line);
    }

    public List<String> log() { return List.copyOf(log); }
    public FlightBooking booking() { return booking; }
    public BookingDocument document() { return document; }
}
