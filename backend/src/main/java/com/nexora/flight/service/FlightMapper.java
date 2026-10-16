package com.nexora.flight.service;

import com.nexora.flight.dto.FlightDtos.AirportDto;
import com.nexora.flight.dto.FlightDtos.BookingDocumentDto;
import com.nexora.flight.dto.FlightDtos.BookingDto;
import com.nexora.flight.dto.FlightDtos.FlightDto;
import com.nexora.flight.dto.FlightDtos.PassengerDto;
import com.nexora.flight.entity.Airport;
import com.nexora.flight.entity.BookingDocument;
import com.nexora.flight.entity.Flight;
import com.nexora.flight.entity.FlightBooking;

import java.util.List;
import java.util.Set;

public final class FlightMapper {

    private FlightMapper() {
    }

    public static AirportDto toDto(Airport a) {
        return new AirportDto(a.getId(), a.getCode(), a.getName(), a.getCity(), a.getCountry(), a.getTimezone());
    }

    public static FlightDto toDto(Flight f) {
        return new FlightDto(f.getId(), f.getFlightNumber(), f.getAirline(), toDto(f.getOrigin()), toDto(f.getDestination()),
                f.getDepartureTime(), f.getArrivalTime(), f.duration().toMinutes(), f.getBasePrice(), f.getAvailableSeats(),
                f.getTotalSeats(), f.getStatus());
    }

    public static BookingDto toDto(FlightBooking b, Set<String> actions, BookingDocument doc, List<String> log) {
        BookingDocumentDto docDto = doc == null ? null
                : new BookingDocumentDto(doc.getDocumentNumber(), doc.getDocumentId(), doc.getContent(), doc.getIssuedAt());
        return new BookingDto(b.getId(), b.getReference(), toDto(b.getFlight()), b.getUserId(), b.getPassengerCount(),
                b.getTotalPrice(), b.getContactEmail(), b.getStatus(), actions, b.getExpiresAt(), b.getConfirmedAt(),
                b.getCancelledAt(), b.getCancelReason(),
                b.getPassengers().stream().map(p -> new PassengerDto(p.getFullName(), p.getSeatNumber())).toList(),
                docDto, b.getCreatedAt(), log);
    }
}
