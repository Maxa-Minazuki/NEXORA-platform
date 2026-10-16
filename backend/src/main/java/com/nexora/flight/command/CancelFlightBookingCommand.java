package com.nexora.flight.command;

import com.nexora.flight.dto.FlightDtos.BookingDto;
import com.nexora.flight.facade.BookingFacade;
import com.nexora.shared.command.Command;
import com.nexora.shared.security.AuthenticatedUser;

/** Command: cancel a booking. The State object decides whether cancellation is allowed. */
public final class CancelFlightBookingCommand implements Command<BookingDto> {

    private final BookingFacade receiver;
    private final Long bookingId;
    private final String reason;
    private final AuthenticatedUser user;

    public CancelFlightBookingCommand(BookingFacade receiver, Long bookingId, String reason, AuthenticatedUser user) {
        this.receiver = receiver;
        this.bookingId = bookingId;
        this.reason = reason;
        this.user = user;
    }

    @Override public String name() { return "CANCEL_FLIGHT_BOOKING"; }
    @Override public String entityType() { return "FlightBooking"; }
    @Override public BookingDto execute() { return receiver.cancel(bookingId, reason, user); }
    @Override public String entityId(BookingDto result) { return String.valueOf(bookingId); }
    @Override public String details() { return reason; }
}
