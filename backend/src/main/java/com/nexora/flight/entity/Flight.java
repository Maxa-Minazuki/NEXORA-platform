package com.nexora.flight.entity;

import com.nexora.shared.exception.ConflictException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "flights")
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private FlightRoute route;

    @Column(name = "flight_number", nullable = false, length = 10)
    private String flightNumber;

    @Column(nullable = false, length = 80)
    private String airline;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "origin_id")
    private Airport origin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_id")
    private Airport destination;

    @Column(name = "departure_time", nullable = false)
    private Instant departureTime;

    @Column(name = "arrival_time", nullable = false)
    private Instant arrivalTime;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "total_seats", nullable = false)
    private int totalSeats;

    @Column(name = "available_seats", nullable = false)
    private int availableSeats;

    @Column(nullable = false, length = 20)
    private String status;

    @Version
    private long version;

    protected Flight() {
    }

    public Flight(FlightRoute route, Instant departure) {
        this.route = route;
        this.flightNumber = route.getFlightNumber();
        this.airline = route.getAirline();
        this.origin = route.getOrigin();
        this.destination = route.getDestination();
        this.departureTime = departure;
        this.arrivalTime = departure.plus(Duration.ofMinutes(route.getDurationMinutes()));
        this.basePrice = route.getBasePrice();
        this.totalSeats = route.getSeatRows() * route.getSeatsPerRow();
        this.availableSeats = totalSeats;
        this.status = "SCHEDULED";
    }

    public void reserveSeats(int count) {
        if (availableSeats < count) {
            throw new ConflictException("NOT_ENOUGH_SEATS", flightNumber + " рейсінде тек " + availableSeats + " бос орын қалды");
        }
        availableSeats -= count;
    }

    public void releaseSeats(int count) {
        availableSeats = Math.min(totalSeats, availableSeats + count);
    }

    public Duration duration() {
        return Duration.between(departureTime, arrivalTime);
    }

    public String routeLabel() {
        return origin.getCode() + " -> " + destination.getCode();
    }

    public Long getId() { return id; }
    public String getFlightNumber() { return flightNumber; }
    public String getAirline() { return airline; }
    public Airport getOrigin() { return origin; }
    public Airport getDestination() { return destination; }
    public Instant getDepartureTime() { return departureTime; }
    public Instant getArrivalTime() { return arrivalTime; }
    public BigDecimal getBasePrice() { return basePrice; }
    public int getTotalSeats() { return totalSeats; }
    public int getAvailableSeats() { return availableSeats; }
    public String getStatus() { return status; }
}
