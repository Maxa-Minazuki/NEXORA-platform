package com.nexora.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;

/**
 * Raw, type-safe binding of the {@code nexora.*} properties. Consumers never use this record
 * directly; they go through the {@link PlatformSettings} singleton, which derives the values the
 * business processes need.
 */
@ConfigurationProperties(prefix = "nexora")
public record NexoraProperties(
        @DefaultValue("NEXORA") String platformName,
        @DefaultValue("Asia/Almaty") String businessTimezone,
        @DefaultValue("15") int bookingHoldMinutes,
        @DefaultValue("30") int appointmentSlotMinutes,
        @DefaultValue("250000.00") BigDecimal managerApprovalThreshold,
        @DefaultValue("1500000.00") BigDecimal additionalApprovalThreshold,
        @DefaultValue("minimum-level") String lowStockPolicy,
        @DefaultValue("14") int flightScheduleHorizonDays,
        @DefaultValue("false") boolean emailEnabled,
        @DefaultValue("no-reply@nexora.local") String emailFrom) {
}
