package com.nexora.flight.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "flight_seats")
public class FlightSeat {

    public enum Status { AVAILABLE, HELD, BOOKED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flight_id")
    private Flight flight;

    @Column(name = "seat_number", nullable = false, length = 4)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Status status = Status.AVAILABLE;

    @Column(name = "booking_id")
    private Long bookingId;

    @Version
    private long version;

    protected FlightSeat() {
    }

    public FlightSeat(Flight flight, String seatNumber) {
        this.flight = flight;
        this.seatNumber = seatNumber;
    }

    public void hold() { this.status = Status.HELD; }
    public void assignTo(Long bookingId) { this.bookingId = bookingId; }
    public void book() { this.status = Status.BOOKED; }

    public void release() {
        this.status = Status.AVAILABLE;
        this.bookingId = null;
    }

    public Long getId() { return id; }
    public Flight getFlight() { return flight; }
    public String getSeatNumber() { return seatNumber; }
    public Status getStatus() { return status; }
    public Long getBookingId() { return bookingId; }
}
