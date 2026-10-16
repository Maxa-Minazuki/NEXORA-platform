package com.nexora.flight.dto;

import com.nexora.flight.entity.BookingStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public final class FlightDtos {

    private FlightDtos() {
    }

    public record AirportDto(Long id, String code, String name, String city, String country, String timezone) {
    }

    public record FlightDto(Long id, String flightNumber, String airline, AirportDto origin, AirportDto destination,
                            Instant departureTime, Instant arrivalTime, long durationMinutes, BigDecimal price,
                            int availableSeats, int totalSeats, String status) {
    }

    public record SearchResult(String sort, String sortLabel, List<String> availableSorts, List<FlightDto> flights,
                               String disclaimer) {
    }

    public record SeatDto(String seatNumber, String status) {
    }

    public record PassengerRequest(@NotBlank @Size(max = 120) String fullName,
                                   @Pattern(regexp = "^$|^[0-9]{1,2}[A-F]$", message = "орын нөмірі 12C пішімінде болуы керек") String seatNumber) {
    }

    public record CreateBookingRequest(@NotNull Long flightId,
                                       @NotEmpty @Size(max = 6) List<@Valid PassengerRequest> passengers,
                                       @NotBlank @Email @Size(max = 120) String contactEmail) {
    }

    public record CancelBookingRequest(@NotBlank @Size(max = 255) String reason) {
    }

    public record PassengerDto(String fullName, String seatNumber) {
    }

    public record BookingDocumentDto(String documentNumber, Long workflowDocumentId, String content, Instant issuedAt) {
    }

    public record BookingDto(Long id, String reference, FlightDto flight, Long userId, int passengerCount,
                             BigDecimal totalPrice, String contactEmail, BookingStatus status, Set<String> allowedActions,
                             Instant expiresAt, Instant confirmedAt, Instant cancelledAt, String cancelReason,
                             List<PassengerDto> passengers, BookingDocumentDto confirmation, Instant createdAt,
                             List<String> coordinationLog) {
    }
}
