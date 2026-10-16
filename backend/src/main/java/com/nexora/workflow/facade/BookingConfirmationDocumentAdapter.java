package com.nexora.workflow.facade;

import com.nexora.flight.port.ConfirmationDocumentPort;
import com.nexora.workflow.entity.Document;
import com.nexora.workflow.entity.DocumentType;
import org.springframework.stereotype.Component;

/** Implements the flight module's outbound port with a system-issued workflow document. */
@Component
public class BookingConfirmationDocumentAdapter implements ConfirmationDocumentPort {

    private final DocumentWorkflowFacade facade;

    public BookingConfirmationDocumentAdapter(DocumentWorkflowFacade facade) {
        this.facade = facade;
    }

    @Override
    public IssuedDocument issueBookingConfirmation(Long authorUserId, Long bookingId, String title, String content) {
        Document d = facade.issueSystemDocument(DocumentType.FLIGHT_BOOKING_CONFIRMATION, title, content, authorUserId,
                "FLIGHT_BOOKING", bookingId);
        return new IssuedDocument(d.getId(), d.getNumber());
    }
}
