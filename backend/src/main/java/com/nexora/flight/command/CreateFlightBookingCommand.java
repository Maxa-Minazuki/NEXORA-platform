package com.nexora.flight.command;

import com.nexora.flight.dto.FlightDtos.BookingDto;
import com.nexora.flight.dto.FlightDtos.CreateBookingRequest;
import com.nexora.flight.facade.BookingFacade;
import com.nexora.shared.command.Command;
import com.nexora.shared.security.AuthenticatedUser;

public final class CreateFlightBookingCommand implements Command<BookingDto> {

    private final BookingFacade receiver;
    private final CreateBookingRequest request;
    private final AuthenticatedUser user;

    public CreateFlightBookingCommand(BookingFacade receiver, CreateBookingRequest request, AuthenticatedUser user) {
        this.receiver = receiver;
        this.request = request;
        this.user = user;
    }

    @Override public String name() { return "CREATE_FLIGHT_BOOKING"; }
    @Override public String entityType() { return "FlightBooking"; }
    @Override public BookingDto execute() { return receiver.create(request, user); }
    @Override public String entityId(BookingDto result) { return result == null ? null : String.valueOf(result.id()); }
    @Override public String details() { return "рейс " + request.flightId() + ", жолаушылар: " + request.passengers().size(); }
}
