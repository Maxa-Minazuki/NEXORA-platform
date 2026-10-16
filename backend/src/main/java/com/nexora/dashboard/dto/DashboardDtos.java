package com.nexora.dashboard.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record Kpis(Long products, BigDecimal stockValue, Long lowStock, Long outOfStock, long doctors,
                       long appointmentsToday, long upcomingAppointments, long bookableFlights, long bookings,
                       long documentsInApproval, long awaitingMyAction, long unreadNotifications) {
    }

    public record MovementPoint(LocalDate date, long inbound, long outbound) {
    }

    public record CountPoint(LocalDate date, long count) {
    }

    public record SeverityPoint(LocalDate date, long info, long success, long warning, long critical) {
    }

    public record Slice(String name, long value) {
    }

    public record Upcoming(Long id, String patient, String doctor, String specialty, Instant start, String status) {
    }

    public record Activity(Long id, String actor, String action, String entityType, String entityId, String outcome,
                           String details, Instant at) {
    }

    public record AttentionItem(String kind, String title, String detail, String link, String severity) {
    }

    public record DashboardView(String greetingName, String role, Instant generatedAt, String timezone,
                                List<String> modules, Kpis kpis, List<MovementPoint> inventoryMovement,
                                List<CountPoint> appointmentTrend, List<Slice> bookingDistribution,
                                List<Slice> documentStatus, List<SeverityPoint> notificationActivity,
                                List<Upcoming> upcomingAppointments, List<Activity> recentEvents,
                                List<Activity> recentActions, List<AttentionItem> attention) {
    }
}
