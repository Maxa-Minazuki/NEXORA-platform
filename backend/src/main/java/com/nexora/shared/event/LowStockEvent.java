package com.nexora.shared.event;

import java.time.Instant;

/** Raised when an adjustment pushes a product to or below its critical level. */
public record LowStockEvent(Long productId, String sku, String productName, int quantity, int threshold,
                            int suggestedReorderQuantity, String replenishmentStrategy,
                            Long triggeredByUserId, Instant occurredAt) implements DomainEvent {
    @Override public String module() { return "INVENTORY"; }
    @Override public String summary() {
        return "Қор аз: " + productName + " (" + sku + "), қалдығы " + quantity + ", шегі " + threshold;
    }
}
