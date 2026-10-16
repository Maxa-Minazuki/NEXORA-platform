package com.nexora.flight.mediator;

/** Messages colleagues send to the mediator. The mediator alone decides who reacts to each one. */
public enum BookingSignal {
    CREATE_REQUESTED, FLIGHT_VERIFIED, SEATS_HELD, BOOKING_RECORDED,
    CONFIRM_REQUESTED, STATE_CONFIRMED, SEATS_BOOKED, DOCUMENT_ISSUED,
    CANCEL_REQUESTED, EXPIRE_REQUESTED, STATE_CLOSED, SEATS_RELEASED,
    ANNOUNCED
}
