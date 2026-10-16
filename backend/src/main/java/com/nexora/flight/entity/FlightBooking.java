package com.nexora.flight.entity;

import com.nexora.shared.persistence.AuditableEntity;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * State-pattern context. The current state is persisted as {@link BookingStatus}; the behaviour of
 * that state lives in {@code com.nexora.flight.state}. The status setters are package-private to the
 * entity, reachable only through {@link #moveTo}, which is called exclusively by State objects.
 */
@Entity
@Table(name = "flight_bookings")
public class FlightBooking extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 8)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flight_id")
    private Flight flight;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "passenger_count", nullable = false)
    private int passengerCount;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "contact_email", nullable = false, length = 120)
    private String contactEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Version
    private long version;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingPassenger> passengers = new ArrayList<>();

    protected FlightBooking() {
    }

    public FlightBooking(String reference, Flight flight, Long userId, BigDecimal totalPrice, String contactEmail,
                         Instant expiresAt) {
        this.reference = reference;
        this.flight = flight;
        this.userId = userId;
        this.totalPrice = totalPrice;
        this.contactEmail = contactEmail;
        this.expiresAt = expiresAt;
    }

    public void addPassenger(String fullName, String seatNumber) {
        passengers.add(new BookingPassenger(this, fullName, seatNumber));
        passengerCount = passengers.size();
    }

    /** Transition hook for State objects only. */
    public void moveTo(BookingStatus next, Instant at, String reason) {
        this.status = next;
        switch (next) {
            case CONFIRMED -> this.confirmedAt = at;
            case CANCELLED, EXPIRED -> {
                this.cancelledAt = at;
                this.cancelReason = reason;
            }
            default -> {
            }
        }
    }

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public Flight getFlight() { return flight; }
    public Long getUserId() { return userId; }
    public int getPassengerCount() { return passengerCount; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public String getContactEmail() { return contactEmail; }
    public BookingStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public String getCancelReason() { return cancelReason; }
    public List<BookingPassenger> getPassengers() { return passengers; }
}
