package com.nexora.dashboard.controller;

import com.nexora.dashboard.dto.DashboardDtos.DashboardView;
import com.nexora.dashboard.service.DashboardService;
import com.nexora.shared.config.PlatformSettings;
import com.nexora.shared.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;
    private final PlatformSettings settings;

    public DashboardController(DashboardService service, PlatformSettings settings) {
        this.service = service;
        this.settings = settings;
    }

    @GetMapping
    public DashboardView dashboard() {
        return service.build(CurrentUser.require());
    }

    /** Read-only view of the platform configuration held by the PlatformSettings singleton. */
    @GetMapping("/settings")
    public Map<String, Object> settings() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("platformName", settings.platformName());
        m.put("businessTimezone", settings.businessZone().getId());
        m.put("bookingHoldMinutes", settings.bookingHold().toMinutes());
        m.put("appointmentSlotMinutes", settings.appointmentSlotMinutes());
        m.put("managerApprovalThreshold", settings.managerApprovalThreshold());
        m.put("additionalApprovalThreshold", settings.additionalApprovalThreshold());
        m.put("lowStockPolicy", settings.lowStockPolicy());
        m.put("flightScheduleHorizonDays", settings.flightScheduleHorizonDays());
        m.put("emailEnabled", settings.emailEnabled());
        m.put("singletonInstanceId", settings.instanceId());
        m.put("singletonCreatedAt", settings.createdAt());
        return m;
    }
}
