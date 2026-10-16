package com.nexora.shared.event;

import java.time.Instant;

public record StockAdjustedEvent(Long productId, String sku, String productName, String movementType, int delta,
                                 int quantityAfter, Long userId, Instant occurredAt) implements DomainEvent {
    @Override public String module() { return "INVENTORY"; }
    @Override public String summary() {
        return "Қор қозғалысы " + (delta > 0 ? "+" : "") + delta + ": " + productName + " -> " + quantityAfter;
    }
}
