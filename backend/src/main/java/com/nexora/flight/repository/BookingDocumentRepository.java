package com.nexora.flight.repository;

import com.nexora.flight.entity.BookingDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookingDocumentRepository extends JpaRepository<BookingDocument, Long> {
    Optional<BookingDocument> findByBookingId(Long bookingId);
}
