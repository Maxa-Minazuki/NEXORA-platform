package com.nexora.inventory.strategy;

import com.nexora.inventory.entity.Product;
import com.nexora.inventory.entity.ReplenishmentPolicy;
import com.nexora.inventory.repository.InventoryTransactionRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

/**
 * Uses real consumption: average daily outflow of the last 30 days, times a 21-day coverage
 * window, plus the minimum level as safety stock, minus what is already on hand.
 */
@Component
public class DemandBasedStrategy implements ReplenishmentStrategy {

    static final int LOOKBACK_DAYS = 30;
    static final int COVERAGE_DAYS = 21;

    private final InventoryTransactionRepository transactions;
    private final Clock clock;

    public DemandBasedStrategy(InventoryTransactionRepository transactions, Clock clock) {
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override public ReplenishmentPolicy policy() { return ReplenishmentPolicy.DEMAND_BASED; }

    @Override public String description() { return "30 күндік сұраныс × 21 күн + қауіпсіздік қоры"; }

    @Override
    public int suggestQuantity(Product product) {
        long outflow = transactions.outflowSince(product.getId(), clock.instant().minus(Duration.ofDays(LOOKBACK_DAYS)));
        double daily = outflow / (double) LOOKBACK_DAYS;
        int coverage = (int) Math.ceil(daily * COVERAGE_DAYS);
        return Math.max(coverage + product.getMinStock() - product.getQuantity(), 1);
    }
}
