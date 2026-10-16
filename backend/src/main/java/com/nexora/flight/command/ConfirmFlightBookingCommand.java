package com.nexora.flight.command;

import com.nexora.flight.dto.FlightDtos.BookingDto;
import com.nexora.flight.facade.BookingFacade;
import com.nexora.shared.command.Command;
import com.nexora.shared.security.AuthenticatedUser;

public final class ConfirmFlightBookingCommand implements Command<BookingDto> {

    private final BookingFacade receiver;
    private final Long bookingId;
    private final AuthenticatedUser user;

    public ConfirmFlightBookingCommand(BookingFacade receiver, Long bookingId, AuthenticatedUser user) {
        this.receiver = receiver;
        this.bookingId = bookingId;
        this.user = user;
    }

    @Override public String name() { return "CONFIRM_FLIGHT_BOOKING"; }
    @Override public String entityType() { return "FlightBooking"; }
    @Override public BookingDto execute() { return receiver.confirm(bookingId, user); }
    @Override public String entityId(BookingDto result) { return String.valueOf(bookingId); }
    @Override public String details() { return "Демо растау (төлемсіз)"; }
}
