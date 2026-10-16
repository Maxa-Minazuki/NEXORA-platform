package com.nexora.flight.mediator;

import com.nexora.flight.dto.FlightDtos.PassengerRequest;
import com.nexora.flight.entity.FlightSeat;
import com.nexora.flight.repository.FlightSeatRepository;
import com.nexora.shared.exception.BusinessRuleException;
import com.nexora.shared.exception.ConflictException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Colleague: holds, books and releases concrete seats (runs under the flight row lock). */
@Component
public class SeatAllocator extends BookingColleague {

    private final FlightSeatRepository seats;

    public SeatAllocator(FlightSeatRepository seats) {
        this.seats = seats;
    }

    @Override public String role() { return "SeatAllocator"; }

    void hold(BookingContext ctx) {
        List<String> requested = ctx.passengers.stream().map(PassengerRequest::seatNumber)
                .filter(s -> s != null && !s.isBlank()).map(String::toUpperCase).toList();
        Set<String> unique = new HashSet<>(requested);
        if (unique.size() != requested.size()) {
            throw new BusinessRuleException("DUPLICATE_SEAT", "Әр жолаушыға бөлек орын таңдаңыз");
        }
        List<FlightSeat> chosen = new ArrayList<>();
        if (!requested.isEmpty()) {
            List<FlightSeat> found = seats.findByFlightIdAndSeatNumberIn(ctx.flight.getId(), requested);
            if (found.size() != requested.size()) {
                throw new BusinessRuleException("UNKNOWN_SEAT", "Таңдалған орындардың кейбірі бұл ұшақта жоқ");
            }
            for (FlightSeat s : found) {
                if (s.getStatus() != FlightSeat.Status.AVAILABLE) {
                    throw new ConflictException("SEAT_TAKEN", "«" + s.getSeatNumber() + "» орны енді бос емес");
                }
            }
            chosen.addAll(found);
        }
        int missing = ctx.passengers.size() - chosen.size();
        if (missing > 0) {
            List<FlightSeat> free = seats.findByFlightIdAndStatusOrderByIdAsc(ctx.flight.getId(), FlightSeat.Status.AVAILABLE)
                    .stream().filter(s -> !unique.contains(s.getSeatNumber())).limit(missing).toList();
            if (free.size() < missing) {
                throw new ConflictException("NOT_ENOUGH_SEATS", "Бос орындар жеткіліксіз");
            }
            chosen.addAll(free);
        }
        chosen.forEach(FlightSeat::hold);
        ctx.flight.reserveSeats(chosen.size());
        ctx.seats = chosen;
        ctx.log(role() + ": ұсталған орындар " + chosen.stream().map(FlightSeat::getSeatNumber).toList());
        report(BookingSignal.SEATS_HELD, ctx);
    }

    void book(BookingContext ctx) {
        List<FlightSeat> held = seats.findByBookingId(ctx.booking.getId());
        held.forEach(FlightSeat::book);
        ctx.log(role() + ": " + held.stream().map(FlightSeat::getSeatNumber).toList() + " орындары брондалды");
        report(BookingSignal.SEATS_BOOKED, ctx);
    }

    void release(BookingContext ctx) {
        List<FlightSeat> held = seats.findByBookingId(ctx.booking.getId());
        held.forEach(FlightSeat::release);
        ctx.flight.releaseSeats(held.size());
        ctx.log(role() + ": " + held.size() + " орын қайта босатылды");
        report(BookingSignal.SEATS_RELEASED, ctx);
    }
}
