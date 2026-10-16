package com.nexora.dashboard.service;

import com.nexora.audit.entity.AuditLog;
import com.nexora.audit.repository.AuditLogRepository;
import com.nexora.clinic.entity.Appointment;
import com.nexora.clinic.entity.AppointmentStatus;
import com.nexora.clinic.repository.AppointmentRepository;
import com.nexora.clinic.repository.DoctorRepository;
import com.nexora.clinic.service.ClinicAccessPolicy;
import com.nexora.dashboard.dto.DashboardDtos.Activity;
import com.nexora.dashboard.dto.DashboardDtos.AttentionItem;
import com.nexora.dashboard.dto.DashboardDtos.CountPoint;
import com.nexora.dashboard.dto.DashboardDtos.DashboardView;
import com.nexora.dashboard.dto.DashboardDtos.Kpis;
import com.nexora.dashboard.dto.DashboardDtos.MovementPoint;
import com.nexora.dashboard.dto.DashboardDtos.SeverityPoint;
import com.nexora.dashboard.dto.DashboardDtos.Slice;
import com.nexora.dashboard.dto.DashboardDtos.Upcoming;
import com.nexora.flight.repository.FlightBookingRepository;
import com.nexora.flight.repository.FlightRepository;
import com.nexora.inventory.dto.InventoryDtos.StockAlertDto;
import com.nexora.inventory.entity.InventoryTransaction;
import com.nexora.inventory.repository.InventoryTransactionRepository;
import com.nexora.inventory.service.InventoryService;
import com.nexora.notification.entity.Notification;
import com.nexora.notification.repository.NotificationRepository;
import com.nexora.shared.config.PlatformSettings;
import com.nexora.shared.security.AuthenticatedUser;
import com.nexora.shared.security.Role;
import com.nexora.workflow.entity.DocumentStatus;
import com.nexora.workflow.repository.DocumentRepository;
import com.nexora.workflow.service.DocumentViewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates live metrics from every module's repositories. Nothing here is hard-coded: an empty
 * database yields zeros and empty series, which the UI renders as empty states.
 */
@Service
public class DashboardService {

    private static final int SERIES_DAYS = 14;

    private final InventoryService inventory;
    private final InventoryTransactionRepository transactions;
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;
    private final ClinicAccessPolicy clinicAccess;
    private final FlightRepository flights;
    private final FlightBookingRepository bookings;
    private final DocumentRepository documents;
    private final DocumentViewService documentViews;
    private final NotificationRepository notifications;
    private final AuditLogRepository audit;
    private final PlatformSettings settings;
    private final Clock clock;

    public DashboardService(InventoryService inventory, InventoryTransactionRepository transactions,
                            DoctorRepository doctors, AppointmentRepository appointments,
                            ClinicAccessPolicy clinicAccess, FlightRepository flights,
                            FlightBookingRepository bookings, DocumentRepository documents,
                            DocumentViewService documentViews, NotificationRepository notifications,
                            AuditLogRepository audit, PlatformSettings settings, Clock clock) {
        this.inventory = inventory;
        this.transactions = transactions;
        this.doctors = doctors;
        this.appointments = appointments;
        this.clinicAccess = clinicAccess;
        this.flights = flights;
        this.bookings = bookings;
        this.documents = documents;
        this.documentViews = documentViews;
        this.notifications = notifications;
        this.audit = audit;
        this.settings = settings;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardView build(AuthenticatedUser user) {
        Instant now = clock.instant();
        ZoneId zone = settings.businessZone();
        LocalDate today = LocalDate.ofInstant(now, zone);
        boolean staff = user.hasRole(Role.ADMIN, Role.MANAGER, Role.EMPLOYEE);
        boolean approver = user.hasRole(Role.ADMIN, Role.MANAGER);

        List<String> modules = new ArrayList<>(List.of("clinic", "flights", "documents", "notifications"));
        if (staff) {
            modules.addFirst("inventory");
        }

        var stats = staff ? inventory.stats() : null;
        long apptsToday = appointments.countByStartTimeGreaterThanEqualAndStartTimeLessThan(
                today.atStartOfDay(zone).toInstant(), today.plusDays(1).atStartOfDay(zone).toInstant());
        long upcomingCount = appointments.countByStatusInAndStartTimeGreaterThanEqual(AppointmentStatus.activeStatuses(), now);
        long totalBookings = bookings.count();
        long inApproval = documents.countByStatusIn(EnumSet.of(DocumentStatus.PENDING_APPROVAL, DocumentStatus.IN_REVIEW));
        var inbox = approver ? documentViews.awaitingMyAction(user) : List.<com.nexora.workflow.dto.WorkflowDtos.DocumentSummary>of();
        long unread = notifications.countByRecipientIdAndReadAtIsNull(user.id());

        Kpis kpis = new Kpis(stats == null ? null : stats.totalProducts(), stats == null ? null : stats.stockValue(),
                stats == null ? null : stats.lowStockCount(), stats == null ? null : stats.outOfStockCount(),
                doctors.countByActiveTrue(), apptsToday, upcomingCount, flights.countBookable(now), totalBookings,
                inApproval, inbox.size(), unread);

        Instant seriesStart = today.minusDays(SERIES_DAYS - 1).atStartOfDay(zone).toInstant();

        List<Upcoming> upcoming = upcomingFor(user, now);
        List<AttentionItem> attention = new ArrayList<>();
        if (staff) {
            for (StockAlertDto a : inventory.alerts(true).stream().limit(4).toList()) {
                attention.add(new AttentionItem("STOCK_ALERT", "Қор аз: " + a.productName(),
                        "Қалдығы " + a.currentQuantity() + " (шегі " + a.threshold() + "), ұсыныс: " + a.suggestedQuantity()
                                + (a.documentId() != null ? " - өтінім жасалған" : ""),
                        "/inventory?tab=alerts", a.currentQuantity() == 0 ? "CRITICAL" : "WARNING"));
            }
        }
        inbox.stream().limit(4).forEach(d -> attention.add(new AttentionItem("APPROVAL", "Бекітіңіз: " + d.number(),
                d.title() + " - " + d.currentStep(), "/documents/" + d.id(), "INFO")));

        return new DashboardView(user.fullName(), user.role().name(), now, zone.getId(), modules, kpis,
                staff ? movement(seriesStart, today, zone) : List.of(),
                appointmentTrend(today, zone), slices(bookings.countByStatusGrouped()),
                slices(documents.countGroupedByStatus()), notificationActivity(seriesStart, today, zone),
                upcoming, approver ? activities(true) : List.of(), approver ? activities(false) : List.of(), attention);
    }

    private List<Upcoming> upcomingFor(AuthenticatedUser user, Instant now) {
        var patientRestriction = clinicAccess.restrictedPatientId(user);
        var doctorRestriction = clinicAccess.restrictedDoctorId(user);
        return appointments.findTop6ByStatusInAndStartTimeGreaterThanEqualOrderByStartTimeAsc(
                        AppointmentStatus.activeStatuses(), now).stream()
                .filter(a -> patientRestriction.map(p -> p.equals(a.getPatient().getId())).orElse(true))
                .filter(a -> doctorRestriction.map(d -> d.equals(a.getDoctor().getId())).orElse(true))
                .map(DashboardService::toUpcoming).toList();
    }

    private static Upcoming toUpcoming(Appointment a) {
        return new Upcoming(a.getId(), a.getPatient().fullName(), a.getDoctor().getFullName(),
                a.getDoctor().getSpecialty().getName(), a.getStartTime(), a.getStatus().name());
    }

    private List<MovementPoint> movement(Instant since, LocalDate today, ZoneId zone) {
        Map<LocalDate, long[]> byDay = emptyDays(today, 2);
        for (InventoryTransaction t : transactions.findSince(since)) {
            long[] v = byDay.get(LocalDate.ofInstant(t.getCreatedAt(), zone));
            if (v == null) {
                continue;
            }
            if (t.getQuantityChange() > 0) {
                v[0] += t.getQuantityChange();
            } else {
                v[1] += -t.getQuantityChange();
            }
        }
        return byDay.entrySet().stream().map(e -> new MovementPoint(e.getKey(), e.getValue()[0], e.getValue()[1])).toList();
    }

    private List<CountPoint> appointmentTrend(LocalDate today, ZoneId zone) {
        LocalDate start = today.minusDays(6);
        LocalDate end = today.plusDays(7);
        Map<LocalDate, Long> byDay = new LinkedHashMap<>();
        for (LocalDate d = start; d.isBefore(end); d = d.plusDays(1)) {
            byDay.put(d, 0L);
        }
        for (Appointment a : appointments.findStartingBetween(start.atStartOfDay(zone).toInstant(),
                end.atStartOfDay(zone).toInstant())) {
            if (a.getStatus() != AppointmentStatus.CANCELLED) {
                byDay.merge(LocalDate.ofInstant(a.getStartTime(), zone), 1L, Long::sum);
            }
        }
        return byDay.entrySet().stream().map(e -> new CountPoint(e.getKey(), e.getValue())).toList();
    }

    private List<SeverityPoint> notificationActivity(Instant since, LocalDate today, ZoneId zone) {
        Map<LocalDate, long[]> byDay = emptyDays(today, 4);
        for (Notification n : notifications.findSince(since)) {
            long[] v = byDay.get(LocalDate.ofInstant(n.getCreatedAt(), zone));
            if (v != null) {
                v[n.getSeverity().ordinal()]++;
            }
        }
        return byDay.entrySet().stream().map(e -> new SeverityPoint(e.getKey(), e.getValue()[0], e.getValue()[1],
                e.getValue()[2], e.getValue()[3])).toList();
    }

    private static Map<LocalDate, long[]> emptyDays(LocalDate today, int width) {
        Map<LocalDate, long[]> map = new LinkedHashMap<>();
        for (int i = SERIES_DAYS - 1; i >= 0; i--) {
            map.put(today.minusDays(i), new long[width]);
        }
        return map;
    }

    private static List<Slice> slices(List<Object[]> rows) {
        return rows.stream().map(r -> new Slice(String.valueOf(r[0]), ((Number) r[1]).longValue()))
                .sorted((a, b) -> Long.compare(b.value(), a.value())).toList();
    }

    private List<Activity> activities(boolean events) {
        var page = audit.search(null, null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 60,
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")));
        return page.getContent().stream()
                .filter(a -> a.getAction().startsWith("EVENT_") == events)
                .limit(8).map(DashboardService::toActivity).toList();
    }

    private static Activity toActivity(AuditLog a) {
        return new Activity(a.getId(), a.getActorUsername(), a.getAction(), a.getEntityType(), a.getEntityId(),
                a.getOutcome(), a.getDetails(), a.getCreatedAt());
    }

}
