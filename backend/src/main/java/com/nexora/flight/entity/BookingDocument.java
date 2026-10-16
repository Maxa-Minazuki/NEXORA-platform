package com.nexora.flight.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Confirmation document ("demo e-ticket") issued when a booking is confirmed. */
@Entity
@Table(name = "booking_documents")
public class BookingDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false, unique = true)
    private Long bookingId;

    @Column(name = "document_number", nullable = false, unique = true, length = 30)
    private String documentNumber;

    @Column(name = "document_id")
    private Long documentId;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    protected BookingDocument() {
    }

    public BookingDocument(Long bookingId, String documentNumber, Long documentId, String content, Instant issuedAt) {
        this.bookingId = bookingId;
        this.documentNumber = documentNumber;
        this.documentId = documentId;
        this.content = content;
        this.issuedAt = issuedAt;
    }

    public Long getId() { return id; }
    public Long getBookingId() { return bookingId; }
    public String getDocumentNumber() { return documentNumber; }
    public Long getDocumentId() { return documentId; }
    public String getContent() { return content; }
    public Instant getIssuedAt() { return issuedAt; }
}
