package com.nexora.flight.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalTime;

/** Template of a daily scheduled flight; concrete {@link Flight}s are generated from it. */
@Entity
@Table(name = "flight_routes")
public class FlightRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flight_number", nullable = false, unique = true, length = 10)
    private String flightNumber;

    @Column(nullable = false, length = 80)
    private String airline;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "origin_id")
    private Airport origin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_id")
    private Airport destination;

    @Column(name = "departure_local", nullable = false)
    private LocalTime departureLocal;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "seat_rows", nullable = false)
    private int seatRows;

    @Column(name = "seats_per_row", nullable = false)
    private int seatsPerRow;

    @Column(nullable = false)
    private boolean active;

    protected FlightRoute() {
    }

    public Long getId() { return id; }
    public String getFlightNumber() { return flightNumber; }
    public String getAirline() { return airline; }
    public Airport getOrigin() { return origin; }
    public Airport getDestination() { return destination; }
    public LocalTime getDepartureLocal() { return departureLocal; }
    public int getDurationMinutes() { return durationMinutes; }
    public BigDecimal getBasePrice() { return basePrice; }
    public int getSeatRows() { return seatRows; }
    public int getSeatsPerRow() { return seatsPerRow; }
    public boolean isActive() { return active; }
}
