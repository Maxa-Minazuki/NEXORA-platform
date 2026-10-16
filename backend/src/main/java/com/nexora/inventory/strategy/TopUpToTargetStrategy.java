package com.nexora.inventory.strategy;

import com.nexora.inventory.entity.Product;
import com.nexora.inventory.entity.ReplenishmentPolicy;
import org.springframework.stereotype.Component;

/** Order enough to bring stock back to three times the minimum level. */
@Component
public class TopUpToTargetStrategy implements ReplenishmentStrategy {

    static final int TARGET_MULTIPLIER = 3;

    @Override public ReplenishmentPolicy policy() { return ReplenishmentPolicy.TOP_UP_TO_TARGET; }

    @Override public String description() { return "Ең төменгі қордың 3 есесіне дейін толықтыру"; }

    @Override
    public int suggestQuantity(Product product) {
        int target = Math.max(product.getMinStock() * TARGET_MULTIPLIER, 1);
        return Math.max(target - product.getQuantity(), 1);
    }
}
