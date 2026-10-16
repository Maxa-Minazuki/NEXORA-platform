package com.nexora.inventory.strategy;

import com.nexora.inventory.entity.Product;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/** Critical when quantity reaches the configured minimum stock. */
@Component
public class MinimumLevelStrategy implements LowStockDetectionStrategy {

    @Override public String key() { return "minimum-level"; }

    @Override public String description() { return "Қор ең төменгі деңгейге жеткенде немесе одан төмендегенде ескерту"; }

    @Override public int criticalThreshold(int minStock) { return minStock; }

    @Override
    public Specification<Product> criticalSpecification() {
        return (root, q, cb) -> cb.lessThanOrEqualTo(root.get("quantity"), root.<Integer>get("minStock"));
    }
}
