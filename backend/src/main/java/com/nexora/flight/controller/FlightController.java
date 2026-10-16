package com.nexora.flight.controller;

import com.nexora.flight.command.CancelFlightBookingCommand;
import com.nexora.flight.command.ConfirmFlightBookingCommand;
import com.nexora.flight.command.CreateFlightBookingCommand;
import com.nexora.flight.dto.FlightDtos.AirportDto;
import com.nexora.flight.dto.FlightDtos.BookingDto;
import com.nexora.flight.dto.FlightDtos.CancelBookingRequest;
import com.nexora.flight.dto.FlightDtos.CreateBookingRequest;
import com.nexora.flight.dto.FlightDtos.FlightDto;
import com.nexora.flight.dto.FlightDtos.SearchResult;
import com.nexora.flight.dto.FlightDtos.SeatDto;
import com.nexora.flight.entity.BookingStatus;
import com.nexora.flight.facade.BookingFacade;
import com.nexora.shared.command.CommandInvoker;
import com.nexora.shared.security.CurrentUser;
import com.nexora.shared.web.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@Validated
public class FlightController {

    private final BookingFacade facade;
    private final CommandInvoker invoker;

    public FlightController(BookingFacade facade, CommandInvoker invoker) {
        this.facade = facade;
        this.invoker = invoker;
    }

    @GetMapping("/api/flights/airports")
    public List<AirportDto> airports() {
        return facade.airports();
    }

    @GetMapping("/api/flights/search")
    public SearchResult search(@RequestParam @Pattern(regexp = "^[A-Za-z]{3}$") String from,
                               @RequestParam @Pattern(regexp = "^[A-Za-z]{3}$") String to,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               @RequestParam(defaultValue = "1") @Min(1) @Max(6) int passengers,
                               @RequestParam(defaultValue = "price") String sort) {
        return facade.search(from, to, date, passengers, sort);
    }

    @GetMapping("/api/flights/{id}")
    public FlightDto flight(@PathVariable Long id) {
        return facade.flight(id);
    }

    @GetMapping("/api/flights/{id}/seats")
    public List<SeatDto> seats(@PathVariable Long id) {
        return facade.seatMap(id);
    }

    @GetMapping("/api/bookings")
    public PageResponse<BookingDto> bookings(@RequestParam(required = false) BookingStatus status,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(defaultValue = "false") boolean all,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return facade.history(CurrentUser.require(), status, q, page, size, all);
    }

    @GetMapping("/api/bookings/{id}")
    public BookingDto booking(@PathVariable Long id) {
        return facade.get(id, CurrentUser.require());
    }

    @PostMapping("/api/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingDto create(@Valid @RequestBody CreateBookingRequest request) {
        return invoker.run(new CreateFlightBookingCommand(facade, request, CurrentUser.require()));
    }

    @PostMapping("/api/bookings/{id}/confirm")
    public BookingDto confirm(@PathVariable Long id) {
        return invoker.run(new ConfirmFlightBookingCommand(facade, id, CurrentUser.require()));
    }

    @PostMapping("/api/bookings/{id}/cancel")
    public BookingDto cancel(@PathVariable Long id, @Valid @RequestBody CancelBookingRequest request) {
        return invoker.run(new CancelFlightBookingCommand(facade, id, request.reason(), CurrentUser.require()));
    }
}
