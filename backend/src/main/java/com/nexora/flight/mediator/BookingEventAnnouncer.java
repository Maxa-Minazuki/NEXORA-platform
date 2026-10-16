package com.nexora.flight.mediator;

import com.nexora.shared.event.FlightBookingCancelledEvent;
import com.nexora.shared.event.FlightBookingConfirmedEvent;
import com.nexora.shared.event.FlightBookingCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Colleague: turns the finished scenario into an Observer event for the rest of the platform. */
@Component
public class BookingEventAnnouncer extends BookingColleague {

    private final ApplicationEventPublisher events;

    public BookingEventAnnouncer(ApplicationEventPublisher events) {
        this.events = events;
    }

    @Override public String role() { return "BookingEventAnnouncer"; }

    void announceCreated(BookingContext ctx) {
        var b = ctx.booking;
        events.publishEvent(new FlightBookingCreatedEvent(b.getId(), b.getReference(), b.getUserId(),
                b.getFlight().getFlightNumber(), b.getFlight().routeLabel(), b.getExpiresAt(), ctx.now));
        ctx.log(role() + ": FlightBookingCreatedEvent жарияланды");
        report(BookingSignal.ANNOUNCED, ctx);
    }

    void announceConfirmed(BookingContext ctx) {
        var b = ctx.booking;
        events.publishEvent(new FlightBookingConfirmedEvent(b.getId(), b.getReference(), b.getUserId(),
                b.getFlight().getFlightNumber(), b.getFlight().routeLabel(), b.getFlight().getDepartureTime(),
                ctx.document == null ? null : ctx.document.getDocumentNumber(), ctx.now));
        ctx.log(role() + ": FlightBookingConfirmedEvent жарияланды");
        report(BookingSignal.ANNOUNCED, ctx);
    }

    void announceClosed(BookingContext ctx) {
        var b = ctx.booking;
        events.publishEvent(new FlightBookingCancelledEvent(b.getId(), b.getReference(), b.getUserId(),
                b.getFlight().getFlightNumber(), b.getCancelReason(), ctx.expired, ctx.now));
        ctx.log(role() + ": FlightBookingCancelledEvent жарияланды" + (ctx.expired ? " (мерзімі өтті)" : ""));
        report(BookingSignal.ANNOUNCED, ctx);
    }
}
