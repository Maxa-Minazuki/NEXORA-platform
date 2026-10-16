package com.nexora.integration;

import com.nexora.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Scenario 12 and authentication rules: backend enforcement, not hidden buttons. */
class SecurityIntegrationTest extends IntegrationTestBase {

    @Test
    void anonymousRequestsAreRejectedWith401() throws Exception {
        mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void loginWithDemoAccountCreatesSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/auth/login").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"manager\",\"password\":\"Manager#2026\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("MANAGER"));
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("manager"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"manager\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));
    }

    @Test
    void stateChangingRequestWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"manager\",\"password\":\"Manager#2026\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void userRoleCannotAccessInventoryOrAudit() throws Exception {
        getAs("user", "/api/inventory/products").andExpect(status().isForbidden());
        getAs("user", "/api/audit").andExpect(status().isForbidden());
        getAs("doctor", "/api/inventory/stats").andExpect(status().isForbidden());
    }

    @Test
    void employeeCannotApproveDocumentsOrManageUsers() throws Exception {
        postAs("employee", "/api/workflows/1/approve", "{}").andExpect(status().isForbidden());
        getAs("employee", "/api/users").andExpect(status().isForbidden());
        getAs("admin", "/api/users").andExpect(status().isOk());
    }

    @Test
    void usersOnlySeeTheirOwnBookings() throws Exception {
        var mine = read(getAs("user", "/api/bookings?all=true"));
        mine.get("content").forEach(b -> org.assertj.core.api.Assertions.assertThat(b.get("userId").asLong())
                .isEqualTo(principal("user").id()));
    }
}
