package com.nexora.flight.mediator;

/**
 * <h2>Design pattern: Mediator</h2>
 *
 * Central contract through which the booking colleagues (flight checker, seat allocator, booking
 * recorder, state keeper, confirmation issuer, event announcer) communicate. Colleagues only talk to
 * the mediator; the mediator holds the interaction rules ("when seats are held, record the booking").
 */
public interface BookingMediator {

    void notify(BookingColleague sender, BookingSignal signal, BookingContext ctx);

    BookingContext create(BookingContext ctx);

    BookingContext confirm(BookingContext ctx);

    BookingContext cancel(BookingContext ctx);

    BookingContext expire(BookingContext ctx);
}
