package com.nexora.inventory.service;

import com.nexora.inventory.entity.StockAlert;
import com.nexora.inventory.repository.ProductRepository;
import com.nexora.inventory.repository.StockAlertRepository;
import com.nexora.shared.event.LowStockEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * <h2>Design pattern: Observer (concrete observer)</h2>
 *
 * Reacts to {@link LowStockEvent} by recording a {@link StockAlert}. A plain {@code @EventListener}
 * runs synchronously inside the publisher's transaction, so the alert is committed atomically with
 * the stock movement that caused it (or rolled back together with it).
 */
@Component
public class LowStockAlertRecorder {

    private final StockAlertRepository alerts;
    private final ProductRepository products;

    public LowStockAlertRecorder(StockAlertRepository alerts, ProductRepository products) {
        this.alerts = alerts;
        this.products = products;
    }

    @EventListener
    public void onLowStock(LowStockEvent event) {
        alerts.save(new StockAlert(products.getReferenceById(event.productId()), event.quantity(), event.threshold(),
                event.suggestedReorderQuantity(), event.replenishmentStrategy(), event.occurredAt()));
    }
}
