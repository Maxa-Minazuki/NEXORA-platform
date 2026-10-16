package com.nexora.integration;

import com.nexora.notification.repository.NotificationRepository;
import com.nexora.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Scenarios 9-11: create, validate (chain), approve (two steps), reject with reason, notifications. */
class WorkflowScenarioTest extends IntegrationTestBase {

    @Autowired
    NotificationRepository notifications;

    private long create(String type, String title, String amount) throws Exception {
        var body = new java.util.HashMap<String, Object>(Map.of("type", type, "title", title,
                "content", "Integration test document with enough content."));
        if (amount != null) {
            body.put("amount", amount);
        }
        return read(postAs("employee", "/api/documents", body).andExpect(status().isCreated())
                .andExpect(jsonPath("$.summary.status").value("DRAFT"))).get("summary").get("id").asLong();
    }

    @Test
    void highValuePurchaseNeedsManagerThenAdmin() throws Exception {
        long id = create("PURCHASE_REQUEST", "Servers for the data room", "2400000.00");
        long managerId = principal("manager").id();
        long before = notifications.countByRecipientIdAndReadAtIsNull(managerId);

        var submitted = read(postAs("employee", "/api/documents/" + id + "/submit", null).andExpect(status().isOk()));
        assertThat(submitted.get("document").get("summary").get("status").asText()).isEqualTo("PENDING_APPROVAL");
        assertThat(submitted.get("document").get("steps")).hasSize(2);
        assertThat(submitted.get("coordinationLog").toString()).contains("ApprovalRoutePlanner");
        assertThat(notifications.countByRecipientIdAndReadAtIsNull(managerId)).isEqualTo(before + 1);

        // the author cannot approve; the manager approves step 1
        postAs("employee", "/api/workflows/" + id + "/approve", "{}").andExpect(status().isForbidden());
        postAs("manager", "/api/workflows/" + id + "/approve", Map.of("comment", "Fine"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.document.summary.status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.document.summary.currentStep").value("Соңғы бекіту"));
        // the manager cannot do the admin's final approval
        postAs("manager", "/api/workflows/" + id + "/approve", "{}").andExpect(status().isForbidden());
        postAs("admin", "/api/workflows/" + id + "/approve", Map.of("comment", "Approved"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.document.summary.status").value("APPROVED"));

        // approved documents are immutable
        postAs("employee", "/api/documents/" + id + "/submit", null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
        var detail = read(getAs("employee", "/api/documents/" + id));
        assertThat(detail.get("track").toString()).contains("Менеджер тексеруі").contains("Соңғы бекіту");
    }

    @Test
    void rejectionRequiresReasonAndNotifiesAuthor() throws Exception {
        long id = create("GENERAL_ADMINISTRATIVE", "Team offsite request", null);
        postAs("employee", "/api/documents/" + id + "/submit", null).andExpect(status().isOk());
        long employeeId = principal("employee").id();
        long before = notifications.countByRecipientIdAndReadAtIsNull(employeeId);

        postAs("manager", "/api/workflows/" + id + "/reject", Map.of("reason", "")).andExpect(status().isBadRequest());
        postAs("manager", "/api/workflows/" + id + "/reject", Map.of("reason", "No budget this quarter"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document.summary.status").value("REJECTED"))
                .andExpect(jsonPath("$.document.rejectionReason").value("No budget this quarter"));
        assertThat(notifications.countByRecipientIdAndReadAtIsNull(employeeId)).isEqualTo(before + 1);
    }

    @Test
    void invalidDocumentIsStoppedByChain() throws Exception {
        var dry = read(postAs("employee", "/api/workflows/validate", Map.of("type", "PURCHASE_REQUEST", "title", "No amount",
                "content", "Content without an amount should fail")));
        assertThat(dry.get("passed").asBoolean()).isFalse();
        assertThat(dry.get("failure").asText()).contains("сома міндетті");
    }

    @Test
    void returnForRevisionAllowsEditAndResubmit() throws Exception {
        long id = create("PURCHASE_REQUEST", "Printer toner", "50000.00");
        postAs("employee", "/api/documents/" + id + "/submit", null).andExpect(status().isOk());
        postAs("manager", "/api/workflows/" + id + "/return", Map.of("comment", "Please add quantities"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.document.summary.status").value("DRAFT"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/documents/" + id)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(principal("employee")))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType("application/json")
                        .content("{\"title\":\"Printer toner x12\",\"content\":\"12 toner cartridges for floor 2\",\"amount\":\"60000.00\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(2));
        postAs("employee", "/api/documents/" + id + "/submit", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.document.approvalRound").value(2));
    }
}
