package com.nexora.notification.listener;

import com.nexora.auth.entity.User;
import com.nexora.auth.repository.UserRepository;
import com.nexora.notification.dto.NotificationDtos.NotificationDraft;
import com.nexora.notification.entity.NotificationCategory;
import com.nexora.notification.entity.Severity;
import com.nexora.notification.service.NotificationDispatcher;
import com.nexora.shared.config.PlatformSettings;
import com.nexora.shared.event.AppointmentBookedEvent;
import com.nexora.shared.event.AppointmentCancelledEvent;
import com.nexora.shared.event.AppointmentRescheduledEvent;
import com.nexora.shared.event.DocumentApprovedEvent;
import com.nexora.shared.event.DocumentRejectedEvent;
import com.nexora.shared.event.DocumentReturnedEvent;
import com.nexora.shared.event.DocumentSubmittedEvent;
import com.nexora.shared.event.FlightBookingCancelledEvent;
import com.nexora.shared.event.FlightBookingConfirmedEvent;
import com.nexora.shared.event.FlightBookingCreatedEvent;
import com.nexora.shared.event.LowStockEvent;
import com.nexora.shared.security.Role;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * <h2>Design pattern: Observer (concrete observer)</h2>
 *
 * Turns business events into notifications. {@code AFTER_COMMIT} guarantees that nobody is told
 * about a booking or approval that was rolled back; each handler runs in a new transaction because
 * the publisher's transaction has already finished. Note: Spring application events are in-memory -
 * if the JVM stops between commit and this listener, the notification is not created (no outbox).
 */
@Component
public class NotificationEventListener {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMMM, EEEE, HH:mm", com.nexora.shared.i18n.Kk.LOCALE);

    private final NotificationDispatcher dispatcher;
    private final UserRepository users;
    private final PlatformSettings settings;

    public NotificationEventListener(NotificationDispatcher dispatcher, UserRepository users, PlatformSettings settings) {
        this.dispatcher = dispatcher;
        this.users = users;
        this.settings = settings;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(LowStockEvent e) {
        Severity severity = e.quantity() == 0 ? Severity.CRITICAL : Severity.WARNING;
        dispatcher.dispatch(usersWithRoles(Role.ADMIN, Role.MANAGER, Role.EMPLOYEE), new NotificationDraft(
                NotificationCategory.INVENTORY, severity,
                (e.quantity() == 0 ? "Тауар таусылды: " : "Қор аз: ") + e.productName(),
                e.productName() + " (" + e.sku() + ") қалдығы: " + e.quantity() + ", шекті деңгей: " + e.threshold()
                        + ". Ұсынылған тапсырыс: " + e.suggestedReorderQuantity() + " бірлік.",
                "/inventory?tab=alerts", "INVENTORY"));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(AppointmentBookedEvent e) {
        String when = format(e.startTime());
        dispatcher.dispatch(listOf(e.patientUserId()), draft(NotificationCategory.CLINIC, Severity.SUCCESS,
                "Қабылдауға жазылдыңыз", e.doctorName() + " дәрігерге жазылуыңыз: " + when + ".", "/clinic", "CLINIC"));
        dispatcher.dispatch(listOf(e.doctorUserId()), draft(NotificationCategory.CLINIC, Severity.INFO,
                "Жаңа қабылдау", e.patientName() + " қабылдауға жазылды: " + when + ".", "/clinic", "CLINIC"));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(AppointmentRescheduledEvent e) {
        String msg = e.patientName() + " мен " + e.doctorName() + " қабылдауы " + format(e.previousStart())
                + " уақытынан " + format(e.newStart()) + " уақытына ауыстырылды.";
        dispatcher.dispatch(listOf(e.patientUserId(), e.doctorUserId()), draft(NotificationCategory.CLINIC, Severity.INFO,
                "Қабылдау уақыты өзгерді", msg, "/clinic", "CLINIC"));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(AppointmentCancelledEvent e) {
        String msg = e.patientName() + " мен " + e.doctorName() + " арасындағы " + format(e.startTime())
                + " қабылдауы болдырылмады: " + e.reason();
        dispatcher.dispatch(listOf(e.patientUserId(), e.doctorUserId()), draft(NotificationCategory.CLINIC, Severity.WARNING,
                "Қабылдау болдырылмады", msg, "/clinic", "CLINIC"));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(FlightBookingCreatedEvent e) {
        dispatcher.dispatch(listOf(e.userId()), draft(NotificationCategory.FLIGHT, Severity.INFO,
                "Орындар ұсталды: " + e.reference(), e.flightNumber() + " (" + e.route() + ") рейсіндегі " + e.reference()
                        + " брондауы " + format(e.expiresAt()) + " дейін ұсталады. Осы уақытқа дейін растаңыз (демо, төлемсіз).",
                "/flights?tab=bookings", "FLIGHT"));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(FlightBookingConfirmedEvent e) {
        dispatcher.dispatch(listOf(e.userId()), draft(NotificationCategory.FLIGHT, Severity.SUCCESS,
                "Брондау расталды: " + e.reference(), e.flightNumber() + " рейсіндегі " + e.reference() + " демо-брондауы расталды. Ұшу: "
                        + format(e.departureTime()) + ". Растау құжаты: " + e.confirmationNumber() + ".",
                "/flights?tab=bookings", "FLIGHT"));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(FlightBookingCancelledEvent e) {
        dispatcher.dispatch(listOf(e.userId()), draft(NotificationCategory.FLIGHT, Severity.WARNING,
                (e.expired() ? "Брондау мерзімі өтті: " : "Брондау болдырылмады: ") + e.reference(),
                e.flightNumber() + " рейсіндегі " + e.reference() + (e.expired() ? " брондауының мерзімі өтті. " : " брондауы болдырылмады. ")
                        + (e.reason() == null ? "" : e.reason()), "/flights?tab=bookings", "FLIGHT"));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(DocumentSubmittedEvent e) {
        dispatcher.dispatch(approvers(e.nextApproverRole(), e.authorId()), draft(NotificationCategory.WORKFLOW, Severity.INFO,
                "Бекіту қажет: " + e.number(), "«" + e.title() + "» құжаты «" + e.nextStepName() + "» қадамын күтуде.",
                "/documents/" + e.documentId(), "WORKFLOW"));
        dispatcher.dispatch(listOf(e.authorId()), draft(NotificationCategory.WORKFLOW, Severity.INFO,
                "Жіберілді: " + e.number(), "«" + e.title() + "» тексеруден өтіп, «" + e.nextStepName() + "» қадамына жіберілді.",
                "/documents/" + e.documentId(), "WORKFLOW"));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(DocumentApprovedEvent e) {
        if (e.finalApproval()) {
            dispatcher.dispatch(listOf(e.authorId()), draft(NotificationCategory.WORKFLOW, Severity.SUCCESS,
                    "Бекітілді: " + e.number(), "«" + e.title() + "» құжаты бекіту маршрутын толық өтті.",
                    "/documents/" + e.documentId(), "WORKFLOW"));
            return;
        }
        dispatcher.dispatch(listOf(e.authorId()), draft(NotificationCategory.WORKFLOW, Severity.INFO,
                "Қадам бекітілді: " + e.number(), "«" + e.title() + "» құжаты «" + e.nextStepName() + "» қадамына өтті.",
                "/documents/" + e.documentId(), "WORKFLOW"));
        dispatcher.dispatch(approvers(e.nextApproverRole(), e.authorId()), draft(NotificationCategory.WORKFLOW, Severity.INFO,
                "Бекіту қажет: " + e.number(), "«" + e.title() + "» құжаты «" + e.nextStepName() + "» қадамын күтуде.",
                "/documents/" + e.documentId(), "WORKFLOW"));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(DocumentRejectedEvent e) {
        dispatcher.dispatch(listOf(e.authorId()), draft(NotificationCategory.WORKFLOW, Severity.WARNING,
                "Қабылданбады: " + e.number(), "«" + e.title() + "» құжаты қабылданбады. Себебі: " + e.reason(),
                "/documents/" + e.documentId(), "WORKFLOW"));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(DocumentReturnedEvent e) {
        dispatcher.dispatch(listOf(e.authorId()), draft(NotificationCategory.WORKFLOW, Severity.WARNING,
                "Пысықтауға қайтарылды: " + e.number(), "«" + e.title() + "» құжатын түзету қажет: " + e.comment(),
                "/documents/" + e.documentId(), "WORKFLOW"));
    }

    private List<Long> approvers(String role, Long excludeUserId) {
        if (role == null) {
            return List.of();
        }
        Role r = Role.valueOf(role);
        return users.findByRoleInAndActiveTrue(EnumSet.of(r)).stream().map(User::getId)
                .filter(id -> !id.equals(excludeUserId)).toList();
    }

    private List<Long> usersWithRoles(Role... roles) {
        return users.findByRoleInAndActiveTrue(List.of(roles)).stream().map(User::getId).toList();
    }

    private static List<Long> listOf(Long... ids) {
        List<Long> list = new ArrayList<>();
        for (Long id : ids) {
            if (id != null) {
                list.add(id);
            }
        }
        return list.stream().filter(Objects::nonNull).distinct().toList();
    }

    private String format(Instant instant) {
        ZoneId zone = settings.businessZone();
        return instant == null ? "-" : WHEN.format(instant.atZone(zone));
    }

    private static NotificationDraft draft(NotificationCategory c, Severity s, String title, String message, String link,
                                           String module) {
        return new NotificationDraft(c, s, title, message, link, module);
    }
}
