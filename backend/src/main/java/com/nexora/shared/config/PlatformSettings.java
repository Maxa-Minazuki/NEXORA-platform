package com.nexora.shared.config;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

/**
 * <h2>Design pattern: Singleton</h2>
 *
 * One shared, immutable source of truth for the business-process configuration of the platform
 * (booking hold window, approval thresholds, active low-stock policy, business time zone, e-mail
 * availability...).
 *
 * <p>Why a single instance is required: the approval chain, the flight hold timer, the clinic
 * slot generator, the inventory alerting and the notification channels must all agree on the
 * <em>same</em> rules. If every module created its own copy, a threshold changed in one place
 * could silently diverge from another.</p>
 *
 * <p>Implementation: the bean has Spring's default {@code singleton} scope, i.e. exactly one
 * instance per {@code ApplicationContext} (not per JVM). It holds no per-user or per-transaction
 * state, only final values, so sharing it across threads is safe. Nobody calls {@code new
 * PlatformSettings(...)}; every consumer receives the same instance through constructor
 * injection. {@link #instanceId()} lets the Design Patterns Lab prove the identity at runtime.</p>
 */
@Component
public class PlatformSettings {

    private final String platformName;
    private final ZoneId businessZone;
    private final Duration bookingHold;
    private final int appointmentSlotMinutes;
    private final BigDecimal managerApprovalThreshold;
    private final BigDecimal additionalApprovalThreshold;
    private final String lowStockPolicy;
    private final int flightScheduleHorizonDays;
    private final boolean emailEnabled;
    private final String emailFrom;
    private final String instanceId = UUID.randomUUID().toString();
    private final Instant createdAt = Instant.now();

    public PlatformSettings(NexoraProperties properties) {
        this.platformName = properties.platformName();
        this.businessZone = ZoneId.of(properties.businessTimezone());
        this.bookingHold = Duration.ofMinutes(properties.bookingHoldMinutes());
        this.appointmentSlotMinutes = properties.appointmentSlotMinutes();
        this.managerApprovalThreshold = properties.managerApprovalThreshold();
        this.additionalApprovalThreshold = properties.additionalApprovalThreshold();
        this.lowStockPolicy = properties.lowStockPolicy();
        this.flightScheduleHorizonDays = properties.flightScheduleHorizonDays();
        this.emailEnabled = properties.emailEnabled();
        this.emailFrom = properties.emailFrom();
    }

    public String platformName() { return platformName; }
    public ZoneId businessZone() { return businessZone; }
    public Duration bookingHold() { return bookingHold; }
    public int appointmentSlotMinutes() { return appointmentSlotMinutes; }
    public BigDecimal managerApprovalThreshold() { return managerApprovalThreshold; }
    public BigDecimal additionalApprovalThreshold() { return additionalApprovalThreshold; }
    public String lowStockPolicy() { return lowStockPolicy; }
    public int flightScheduleHorizonDays() { return flightScheduleHorizonDays; }
    public boolean emailEnabled() { return emailEnabled; }
    public String emailFrom() { return emailFrom; }
    public String instanceId() { return instanceId; }
    public Instant createdAt() { return createdAt; }
}
