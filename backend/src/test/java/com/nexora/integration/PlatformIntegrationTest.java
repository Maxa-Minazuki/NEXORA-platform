package com.nexora.integration;

import com.nexora.shared.config.PlatformSettings;
import com.nexora.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Singleton, dashboard aggregation, notifications API and the Design Patterns Lab endpoints. */
class PlatformIntegrationTest extends IntegrationTestBase {

    @Autowired
    ApplicationContext context;

    @Test
    void platformSettingsIsASingleSharedInstance() throws Exception {
        PlatformSettings a = context.getBean(PlatformSettings.class);
        PlatformSettings b = context.getBean(PlatformSettings.class);
        assertThat(a).isSameAs(b);
        getAs("employee", "/api/lab/singleton").andExpect(status().isOk())
                .andExpect(jsonPath("$.sameInstance").value(true))
                .andExpect(jsonPath("$.injectedInstanceId").value(a.instanceId()));
    }

    @Test
    void dashboardReturnsDatabaseBackedMetrics() throws Exception {
        var d = read(getAs("manager", "/api/dashboard").andExpect(status().isOk()));
        assertThat(d.get("kpis").get("products").asLong()).isGreaterThan(0);
        assertThat(d.get("kpis").get("doctors").asLong()).isEqualTo(6);
        assertThat(d.get("inventoryMovement")).hasSize(14);
        assertThat(d.get("documentStatus").size()).isGreaterThan(0);
        // USER role does not receive inventory figures
        var u = read(getAs("user", "/api/dashboard"));
        assertThat(u.get("kpis").get("stockValue").isNull()).isTrue();
    }

    @Test
    void notificationsCanBeReadAndStrategyReportsChannels() throws Exception {
        var report = read(postAs("doctor", "/api/notifications/test",
                java.util.Map.of("severity", "CRITICAL", "title", "Test", "message", "Critical test")).andExpect(status().isOk()));
        assertThat(report.get("strategy").asText()).isEqualTo("CriticalEscalation");
        assertThat(report.get("selectedChannels").toString()).contains("IN_APP").contains("EMAIL");
        assertThat(report.get("attempts").toString()).contains("SKIPPED"); // no SMTP in tests
        postAs("doctor", "/api/notifications/read-all", null).andExpect(status().isOk());
        getAs("doctor", "/api/notifications/unread-count").andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void labScenariosRunAgainstRealServices() throws Exception {
        var low = read(postAs("employee", "/api/lab/low-stock", null).andExpect(status().isOk()));
        assertThat(low.get("steps").toString()).contains("LowStockAlertRecorder");
        var booking = read(postAs("user", "/api/lab/booking-lifecycle", null).andExpect(status().isOk()));
        assertThat(booking.get("forbiddenTransition").asText()).contains("INVALID_STATE_TRANSITION");
        assertThat(booking.get("createLog").toString()).contains("SeatAllocator");
        var wf = read(postAs("employee", "/api/lab/workflow-submit", null).andExpect(status().isOk()));
        assertThat(wf.get("status").asText()).isEqualTo("PENDING_APPROVAL");
        getAs("employee", "/api/lab/my-commands").andExpect(status().isOk());
    }
}
