package com.nexora.inventory.strategy;

import com.nexora.inventory.entity.Product;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/** Early warning: critical when quantity is within a 25% safety buffer above the minimum. */
@Component
public class SafetyBufferStrategy implements LowStockDetectionStrategy {

    @Override public String key() { return "safety-buffer"; }

    @Override public String description() { return "Ерте ескерту: ең төменгі қор + 25% қауіпсіздік қоры"; }

    @Override
    public int criticalThreshold(int minStock) {
        return (int) Math.ceil(minStock * 1.25);
    }

    @Override
    public Specification<Product> criticalSpecification() {
        // quantity * 4 <= minStock * 5  <=>  quantity <= 1.25 * minStock (integer-safe, ceil semantics differ by < 1 unit)
        return (root, q, cb) -> cb.lessThanOrEqualTo(
                cb.prod(root.<Integer>get("quantity"), 4), cb.prod(root.<Integer>get("minStock"), 5));
    }
}
