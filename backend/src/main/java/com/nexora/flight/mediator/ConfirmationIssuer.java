package com.nexora.flight.mediator;

import com.nexora.flight.entity.BookingDocument;
import com.nexora.flight.entity.BookingPassenger;
import com.nexora.flight.entity.Flight;
import com.nexora.flight.entity.FlightBooking;
import com.nexora.flight.port.ConfirmationDocumentPort;
import com.nexora.flight.repository.BookingDocumentRepository;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/** Colleague: issues the confirmation (demo e-ticket) and the matching workflow document. */
@Component
public class ConfirmationIssuer extends BookingColleague {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", com.nexora.shared.i18n.Kk.LOCALE);

    private final BookingDocumentRepository documents;
    private final ConfirmationDocumentPort workflow;

    public ConfirmationIssuer(BookingDocumentRepository documents, ConfirmationDocumentPort workflow) {
        this.documents = documents;
        this.workflow = workflow;
    }

    @Override public String role() { return "ConfirmationIssuer"; }

    void issue(BookingContext ctx) {
        FlightBooking b = ctx.booking;
        Flight f = b.getFlight();
        String content = render(b, f);
        var issued = workflow.issueBookingConfirmation(b.getUserId(), b.getId(),
                "Әуе билетін брондауды растау " + b.getReference(), content);
        BookingDocument doc = documents.save(new BookingDocument(b.getId(), "CNF-" + b.getReference(),
                issued.documentId(), content, ctx.now));
        ctx.document = doc;
        ctx.log(role() + ": " + doc.getDocumentNumber() + " растауы және " + issued.number() + " құжаты шығарылды");
        report(BookingSignal.DOCUMENT_ISSUED, ctx);
    }

    private static String render(FlightBooking b, Flight f) {
        ZoneId originZone = f.getOrigin().zone();
        ZoneId destZone = f.getDestination().zone();
        String passengers = b.getPassengers().stream()
                .map((BookingPassenger p) -> "  - " + p.getFullName() + " (орын " + p.getSeatNumber() + ")")
                .collect(Collectors.joining("\n"));
        return """
                ОҚУ ДЕМОНСТРАЦИЯСЫ - НАҚТЫ БИЛЕТ ЕМЕС. Әуе компаниясында брондау немесе төлем жасалмады.
                Брондау коды: %s
                Рейс: %s %s
                Бағыт: %s (%s) -> %s (%s)
                Ұшу: %s жергілікті уақыт
                Келу: %s жергілікті уақыт
                Жолаушылар:
                %s
                Барлығы (демо баға): %s KZT
                """.formatted(b.getReference(), f.getAirline(), f.getFlightNumber(), f.getOrigin().getCity(),
                f.getOrigin().getCode(), f.getDestination().getCity(), f.getDestination().getCode(),
                FMT.format(f.getDepartureTime().atZone(originZone)), FMT.format(f.getArrivalTime().atZone(destZone)),
                passengers, b.getTotalPrice().toPlainString()).trim();
    }
}
