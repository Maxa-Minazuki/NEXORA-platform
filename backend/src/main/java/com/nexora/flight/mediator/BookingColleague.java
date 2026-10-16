package com.nexora.flight.mediator;

/**
 * Mediator pattern colleague. Colleagues do one job and report the outcome to the mediator; they do
 * not know - and never call - each other.
 */
public abstract class BookingColleague {

    private BookingMediator mediator;

    void attach(BookingMediator mediator) {
        this.mediator = mediator;
    }

    protected void report(BookingSignal signal, BookingContext ctx) {
        mediator.notify(this, signal, ctx);
    }

    public abstract String role();
}
