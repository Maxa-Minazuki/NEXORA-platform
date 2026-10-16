package com.nexora.flight.mediator;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

/** Mediator pattern: the mediator, not the colleagues, decides who acts after each signal. */
class FlightBookingMediatorTest {

    private final FlightAvailabilityChecker checker = mock(FlightAvailabilityChecker.class);
    private final SeatAllocator seats = mock(SeatAllocator.class);
    private final BookingRecorder recorder = mock(BookingRecorder.class);
    private final BookingStateKeeper stateKeeper = mock(BookingStateKeeper.class);
    private final ConfirmationIssuer issuer = mock(ConfirmationIssuer.class);
    private final BookingEventAnnouncer announcer = mock(BookingEventAnnouncer.class);
    private final FlightBookingMediator mediator = new FlightBookingMediator(checker, seats, recorder, stateKeeper, issuer, announcer);

    private final BookingContext ctx = BookingContext.forCreate(1L, 2L, List.of(), "a@b.c", Instant.now());

    @Test
    void creationSignalsAreRoutedInOrder() {
        mediator.create(ctx);
        mediator.notify(checker, BookingSignal.FLIGHT_VERIFIED, ctx);
        mediator.notify(seats, BookingSignal.SEATS_HELD, ctx);
        mediator.notify(recorder, BookingSignal.BOOKING_RECORDED, ctx);

        InOrder order = inOrder(checker, seats, recorder, announcer);
        order.verify(checker).verify(ctx);
        order.verify(seats).hold(ctx);
        order.verify(recorder).record(ctx);
        order.verify(announcer).announceCreated(ctx);
        verify(issuer, never()).issue(any());
        verify(stateKeeper, never()).confirm(any());
    }

    @Test
    void confirmationIssuesDocumentOnlyAfterSeatsAreBooked() {
        mediator.confirm(ctx);
        verify(stateKeeper).confirm(ctx);
        mediator.notify(stateKeeper, BookingSignal.STATE_CONFIRMED, ctx);
        verify(seats).book(ctx);
        verify(issuer, never()).issue(any());
        mediator.notify(seats, BookingSignal.SEATS_BOOKED, ctx);
        verify(issuer).issue(ctx);
        mediator.notify(issuer, BookingSignal.DOCUMENT_ISSUED, ctx);
        verify(announcer).announceConfirmed(ctx);
    }

    @Test
    void cancellationReleasesSeatsThenAnnounces() {
        mediator.cancel(ctx);
        verify(stateKeeper).cancel(ctx);
        mediator.notify(stateKeeper, BookingSignal.STATE_CLOSED, ctx);
        verify(seats).release(ctx);
        mediator.notify(seats, BookingSignal.SEATS_RELEASED, ctx);
        verify(announcer).announceClosed(ctx);
        assertThat(ctx.log()).first().asString().contains("CANCEL_REQUESTED");
    }
}
