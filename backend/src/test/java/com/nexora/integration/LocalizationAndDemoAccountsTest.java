package com.nexora.integration;

import com.nexora.auth.entity.User;
import com.nexora.auth.service.DemoAccountSynchronizer;
import com.nexora.inventory.repository.ProductRepository;
import com.nexora.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Kazakh demo data (Flyway V3), Kazakh API messages and the demo-account repair at startup. */
class LocalizationAndDemoAccountsTest extends IntegrationTestBase {

    @Autowired
    ProductRepository products;

    @Autowired
    DemoAccountSynchronizer synchronizer;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    TransactionTemplate tx;

    @Test
    void flywayV3TranslatesDemoDataWithoutDeletingIt() {
        assertThat(users.findByUsernameIgnoreCase("admin").orElseThrow().getFullName()).isEqualTo("Аружан Садықова");
        var scanner = products.findBySku("ELC-1001").orElseThrow();
        assertThat(scanner.getName()).isEqualTo("Штрихкод сканері Pro X2");
        assertThat(scanner.getUnit()).isEqualTo("дана");
        assertThat(products.count()).isGreaterThanOrEqualTo(22);
    }

    @Test
    void demoAccountListDoesNotExposePasswords() throws Exception {
        String body = mvc.perform(get("/api/auth/demo-accounts")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).contains("\"username\":\"admin\"").doesNotContain("password").doesNotContain("Admin#2026");
    }

    @Test
    void apiMessagesAreKazakh() throws Exception {
        var wrong = read(mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"user\",\"password\":\"nope\"}")).andExpect(status().isUnauthorized()));
        assertThat(wrong.get("message").asText()).isEqualTo("Пайдаланушы аты немесе құпиясөз қате");

        var invalid = read(postAs("employee", "/api/inventory/products", Map.of("sku", "", "unit", "дана"))
                .andExpect(status().isBadRequest()));
        assertThat(invalid.get("message").asText()).isEqualTo("Енгізілген деректер тексеруден өтпеді");
        assertThat(invalid.get("fieldErrors").toString()).contains("бос болмауы керек");
    }

    @Test
    void brokenDemoAccountIsRepairedOnStartupWithoutDuplicates() throws Exception {
        long before = users.count();
        tx.executeWithoutResult(s -> {
            User doctor = users.findByUsernameIgnoreCase("doctor").orElseThrow();
            doctor.changePasswordHash(encoder.encode("something-else"));
            doctor.setActive(false);
        });
        synchronizer.run(null);

        User doctor = users.findByUsernameIgnoreCase("doctor").orElseThrow();
        assertThat(doctor.isActive()).isTrue();
        assertThat(encoder.matches("Doctor#2026", doctor.getPasswordHash())).isTrue();
        assertThat(users.count()).isEqualTo(before);

        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/auth/login").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"doctor\",\"password\":\"Doctor#2026\"}")).andExpect(status().isOk());
        var me = read(mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk()));
        assertThat(me.get("role").asText()).isEqualTo("DOCTOR");
    }
}
