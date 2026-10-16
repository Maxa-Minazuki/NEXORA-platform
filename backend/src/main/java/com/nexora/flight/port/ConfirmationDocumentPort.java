package com.nexora.flight.port;

/**
 * Outbound port: the flight module needs a workflow document for each confirmed booking but must not
 * depend on the workflow module. The workflow module provides the implementation.
 */
public interface ConfirmationDocumentPort {

    IssuedDocument issueBookingConfirmation(Long authorUserId, Long bookingId, String title, String content);

    record IssuedDocument(Long documentId, String number) {
    }
}
