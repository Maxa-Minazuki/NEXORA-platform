package com.nexora.flight.mediator;

import com.nexora.flight.state.BookingState;
import com.nexora.shared.i18n.Kk;
import com.nexora.flight.state.BookingStateRegistry;
import org.springframework.stereotype.Component;

/** Colleague: applies lifecycle operations through the State pattern objects. */
@Component
public class BookingStateKeeper extends BookingColleague {

    private final BookingStateRegistry states;

    public BookingStateKeeper(BookingStateRegistry states) {
        this.states = states;
    }

    @Override public String role() { return "BookingStateKeeper"; }

    void confirm(BookingContext ctx) {
        BookingState state = states.of(ctx.booking);
        state.confirm(ctx.booking, ctx.now);
        ctx.log(role() + ": " + Kk.status(state.status().name()) + " -> " + Kk.status(ctx.booking.getStatus().name()));
        report(BookingSignal.STATE_CONFIRMED, ctx);
    }

    void cancel(BookingContext ctx) {
        BookingState state = states.of(ctx.booking);
        state.cancel(ctx.booking, ctx.reason, ctx.now);
        ctx.log(role() + ": " + Kk.status(state.status().name()) + " -> " + Kk.status(ctx.booking.getStatus().name()));
        report(BookingSignal.STATE_CLOSED, ctx);
    }

    void expire(BookingContext ctx) {
        BookingState state = states.of(ctx.booking);
        state.expire(ctx.booking, ctx.now);
        ctx.expired = true;
        ctx.log(role() + ": " + Kk.status(state.status().name()) + " -> " + Kk.status(ctx.booking.getStatus().name()));
        report(BookingSignal.STATE_CLOSED, ctx);
    }
}
