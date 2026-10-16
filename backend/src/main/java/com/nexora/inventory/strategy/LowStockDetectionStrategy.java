package com.nexora.inventory.strategy;

import com.nexora.inventory.entity.Product;
import org.springframework.data.jpa.domain.Specification;

/**
 * <h2>Design pattern: Strategy - "when is stock critical?"</h2>
 *
 * Interchangeable algorithms for deciding that a product needs attention. The active strategy is
 * chosen by the {@code nexora.low-stock-policy} setting (read from the {@code PlatformSettings}
 * singleton) and resolved by {@link InventoryStrategyResolver}; the inventory service never
 * contains an if/else over policies.
 */
public interface LowStockDetectionStrategy {

    /** Key used in configuration, e.g. {@code minimum-level}. */
    String key();

    String description();

    /** Quantity at or below which the product is critical. */
    int criticalThreshold(int minStock);

    default boolean isCritical(int quantity, int minStock) {
        return quantity <= criticalThreshold(minStock);
    }

    /** Same rule expressed as a JPA predicate, so list filters agree with alerting. */
    Specification<Product> criticalSpecification();
}
