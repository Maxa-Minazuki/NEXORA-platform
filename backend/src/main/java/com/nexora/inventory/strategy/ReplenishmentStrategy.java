package com.nexora.inventory.strategy;

import com.nexora.inventory.entity.Product;
import com.nexora.inventory.entity.ReplenishmentPolicy;

/**
 * <h2>Design pattern: Strategy - "how much should we reorder?"</h2>
 *
 * Each product declares a {@link ReplenishmentPolicy}; the matching strategy computes the
 * suggested reorder quantity stored on the stock alert and pre-filled in the replenishment request.
 */
public interface ReplenishmentStrategy {

    ReplenishmentPolicy policy();

    String description();

    /** Suggested quantity to order, always at least 1. */
    int suggestQuantity(Product product);
}
