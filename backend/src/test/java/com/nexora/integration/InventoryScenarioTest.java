package com.nexora.integration;

import com.nexora.notification.repository.NotificationRepository;
import com.nexora.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Scenarios 1-3: create a product, write it down to the critical level, observe alert + notification. */
class InventoryScenarioTest extends IntegrationTestBase {

    @Autowired
    NotificationRepository notifications;

    private Map<String, Object> product(String sku, int qty, int min) {
        return Map.ofEntries(Map.entry("sku", sku), Map.entry("name", "Test product " + sku),
                Map.entry("description", "integration test"), Map.entry("categoryId", 1), Map.entry("supplierId", 1),
                Map.entry("purchasePrice", "100.00"), Map.entry("retailPrice", "150.00"), Map.entry("quantity", qty),
                Map.entry("minStock", min), Map.entry("unit", "pcs"), Map.entry("replenishmentStrategy", "TOP_UP_TO_TARGET"));
    }

    @Test
    void createProductValidatesAndRejectsDuplicates() throws Exception {
        postAs("employee", "/api/inventory/products", product("IT-CREATE-1", 50, 10))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("IT-CREATE-1"))
                .andExpect(jsonPath("$.stockStatus").value("IN_STOCK"))
                .andExpect(jsonPath("$.stockValue").value(5000.00));
        postAs("employee", "/api/inventory/products", product("IT-CREATE-1", 5, 1))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DUPLICATE_SKU"));
        var invalid = new java.util.HashMap<>(product("bad sku!", -1, 1));
        postAs("employee", "/api/inventory/products", invalid)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void writeOffBelowMinimumTriggersObserverChain() throws Exception {
        var created = read(postAs("employee", "/api/inventory/products", product("IT-LOW-1", 30, 10)));
        long id = created.get("id").asLong();
        long employeeId = principal("employee").id();
        long before = notifications.countByRecipientIdAndReadAtIsNull(employeeId);

        var result = read(postAs("employee", "/api/inventory/products/" + id + "/adjustments",
                Map.of("type", "WRITE_OFF", "quantity", 25, "reason", "integration test"))
                .andExpect(status().isOk()));

        assertThat(result.get("lowStockTriggered").asBoolean()).isTrue();
        assertThat(result.get("product").get("quantity").asInt()).isEqualTo(5);
        assertThat(result.get("product").get("stockStatus").asText()).isEqualTo("LOW_STOCK");

        var alerts = read(getAs("employee", "/api/inventory/alerts"));
        var alert = java.util.stream.StreamSupport.stream(alerts.spliterator(), false)
                .filter(a -> a.get("productSku").asText().equals("IT-LOW-1")).findFirst().orElseThrow();
        assertThat(alert.get("suggestedQuantity").asInt()).isEqualTo(25); // TOP_UP: 3 x 10 - 5
        assertThat(notifications.countByRecipientIdAndReadAtIsNull(employeeId)).isEqualTo(before + 1);

        // a second write-off does not raise a duplicate alert
        var again = read(postAs("employee", "/api/inventory/products/" + id + "/adjustments",
                Map.of("type", "WRITE_OFF", "quantity", 1, "reason", "again")));
        assertThat(again.get("lowStockTriggered").asBoolean()).isFalse();
    }

    @Test
    void stockCannotGoNegative() throws Exception {
        var created = read(postAs("employee", "/api/inventory/products", product("IT-NEG-1", 3, 1)));
        postAs("employee", "/api/inventory/products/" + created.get("id").asLong() + "/adjustments",
                Map.of("type", "WRITE_OFF", "quantity", 10, "reason", "too much"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
        getAs("employee", "/api/inventory/products/" + created.get("id").asLong())
                .andExpect(jsonPath("$.quantity").value(3));
    }

    @Test
    void lowStockFilterUsesActiveStrategy() throws Exception {
        var page = read(getAs("manager", "/api/inventory/products?availability=CRITICAL&size=100"));
        page.get("content").forEach(p -> assertThat(p.get("quantity").asInt())
                .isLessThanOrEqualTo(p.get("criticalThreshold").asInt()));
        assertThat(page.get("totalElements").asInt()).isGreaterThan(0);
    }
}
