package com.nexora.inventory.strategy;

import com.nexora.inventory.entity.Product;
import com.nexora.inventory.entity.ReplenishmentPolicy;
import org.springframework.stereotype.Component;

/** Suppliers ship in fixed lots of 50 units: order whole lots covering twice the minimum level. */
@Component
public class FixedLotStrategy implements ReplenishmentStrategy {

    static final int LOT_SIZE = 50;

    @Override public ReplenishmentPolicy policy() { return ReplenishmentPolicy.FIXED_LOT; }

    @Override public String description() { return "Жеткізушінің 50 бірліктік толық партиялары"; }

    @Override
    public int suggestQuantity(Product product) {
        int needed = Math.max(product.getMinStock() * 2 - product.getQuantity(), 1);
        int lots = (needed + LOT_SIZE - 1) / LOT_SIZE;
        return lots * LOT_SIZE;
    }
}
