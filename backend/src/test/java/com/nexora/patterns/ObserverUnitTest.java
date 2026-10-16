package com.nexora.patterns;

import com.nexora.inventory.entity.Product;
import com.nexora.inventory.entity.StockAlert;
import com.nexora.inventory.repository.ProductRepository;
import com.nexora.inventory.repository.StockAlertRepository;
import com.nexora.inventory.service.LowStockAlertRecorder;
import com.nexora.shared.event.LowStockEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Observer pattern: the alert recorder reacts to the event without the publisher knowing it. */
class ObserverUnitTest {

    @Test
    void lowStockEventCreatesAlertWithSuggestedQuantity() {
        StockAlertRepository alerts = mock(StockAlertRepository.class);
        ProductRepository products = mock(ProductRepository.class);
        Product product = mock(Product.class);
        when(products.getReferenceById(9L)).thenReturn(product);

        new LowStockAlertRecorder(alerts, products).onLowStock(new LowStockEvent(9L, "SKU-9", "Gloves", 3, 10, 27,
                "TOP_UP_TO_TARGET", 4L, Instant.now()));

        ArgumentCaptor<StockAlert> saved = ArgumentCaptor.forClass(StockAlert.class);
        verify(alerts).save(saved.capture());
        assertThat(saved.getValue().getSuggestedQuantity()).isEqualTo(27);
        assertThat(saved.getValue().getStatus()).isEqualTo(StockAlert.Status.OPEN);
        assertThat(saved.getValue().getProduct()).isSameAs(product);
    }
}
