package com.nexora.flight.mediator;

import org.springframework.stereotype.Component;

/**
 * <h2>Design pattern: Mediator (concrete mediator)</h2>
 *
 * Owns the interaction rules between the booking colleagues. Each colleague finishes its own step and
 * calls {@code report(signal)}; this class decides which colleague acts next. Changing the scenario
 * (e.g. adding a fraud check between FLIGHT_VERIFIED and SEATS_HELD) touches only this class.
 *
 * <p>Compare with {@link com.nexora.flight.facade.BookingFacade}: the facade is the simple API that
 * the controller sees; the mediator is the hub that the parts behind the facade use to cooperate.</p>
 */
@Component
public class FlightBookingMediator implements BookingMediator {

    private final FlightAvailabilityChecker checker;
    private final SeatAllocator seats;
    private final BookingRecorder recorder;
    private final BookingStateKeeper stateKeeper;
    private final ConfirmationIssuer issuer;
    private final BookingEventAnnouncer announcer;

    public FlightBookingMediator(FlightAvailabilityChecker checker, SeatAllocator seats, BookingRecorder recorder,
                                 BookingStateKeeper stateKeeper, ConfirmationIssuer issuer,
                                 BookingEventAnnouncer announcer) {
        this.checker = checker;
        this.seats = seats;
        this.recorder = recorder;
        this.stateKeeper = stateKeeper;
        this.issuer = issuer;
        this.announcer = announcer;
        for (BookingColleague c : new BookingColleague[]{checker, seats, recorder, stateKeeper, issuer, announcer}) {
            c.attach(this);
        }
    }

    @Override
    public BookingContext create(BookingContext ctx) {
        notify(null, BookingSignal.CREATE_REQUESTED, ctx);
        return ctx;
    }

    @Override
    public BookingContext confirm(BookingContext ctx) {
        notify(null, BookingSignal.CONFIRM_REQUESTED, ctx);
        return ctx;
    }

    @Override
    public BookingContext cancel(BookingContext ctx) {
        notify(null, BookingSignal.CANCEL_REQUESTED, ctx);
        return ctx;
    }

    @Override
    public BookingContext expire(BookingContext ctx) {
        notify(null, BookingSignal.EXPIRE_REQUESTED, ctx);
        return ctx;
    }

    @Override
    public void notify(BookingColleague sender, BookingSignal signal, BookingContext ctx) {
        ctx.log("Mediator <- " + (sender == null ? "BookingFacade" : sender.role()) + ": " + signal);
        switch (signal) {
            case CREATE_REQUESTED -> checker.verify(ctx);
            case FLIGHT_VERIFIED -> seats.hold(ctx);
            case SEATS_HELD -> recorder.record(ctx);
            case BOOKING_RECORDED -> announcer.announceCreated(ctx);

            case CONFIRM_REQUESTED -> stateKeeper.confirm(ctx);
            case STATE_CONFIRMED -> seats.book(ctx);
            case SEATS_BOOKED -> issuer.issue(ctx);
            case DOCUMENT_ISSUED -> announcer.announceConfirmed(ctx);

            case CANCEL_REQUESTED -> stateKeeper.cancel(ctx);
            case EXPIRE_REQUESTED -> stateKeeper.expire(ctx);
            case STATE_CLOSED -> seats.release(ctx);
            case SEATS_RELEASED -> announcer.announceClosed(ctx);

            case ANNOUNCED -> ctx.log("Mediator: сценарий аяқталды");
        }
    }
}
