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

@Entity
@Table(name = "booking_passengers")
public class BookingPassenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id")
    private FlightBooking booking;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "seat_number", nullable = false, length = 4)
    private String seatNumber;

    protected BookingPassenger() {
    }

    BookingPassenger(FlightBooking booking, String fullName, String seatNumber) {
        this.booking = booking;
        this.fullName = fullName;
        this.seatNumber = seatNumber;
    }

    public String getFullName() { return fullName; }
    public String getSeatNumber() { return seatNumber; }
}
