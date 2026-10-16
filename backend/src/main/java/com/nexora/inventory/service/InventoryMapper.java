package com.nexora.inventory.service;

import com.nexora.inventory.dto.InventoryDtos.ProductDto;
import com.nexora.inventory.dto.InventoryDtos.StockAlertDto;
import com.nexora.inventory.dto.InventoryDtos.StockStatus;
import com.nexora.inventory.dto.InventoryDtos.TransactionDto;
import com.nexora.inventory.entity.InventoryTransaction;
import com.nexora.inventory.entity.Product;
import com.nexora.inventory.entity.StockAlert;
import com.nexora.inventory.strategy.LowStockDetectionStrategy;

final class InventoryMapper {

    private InventoryMapper() {
    }

    static ProductDto toDto(Product p, LowStockDetectionStrategy detection) {
        StockStatus status = p.getQuantity() == 0 ? StockStatus.OUT_OF_STOCK
                : detection.isCritical(p.getQuantity(), p.getMinStock()) ? StockStatus.LOW_STOCK : StockStatus.IN_STOCK;
        return new ProductDto(p.getId(), p.getSku(), p.getName(), p.getDescription(), p.getCategory().getId(),
                p.getCategory().getName(), p.getSupplier() == null ? null : p.getSupplier().getId(),
                p.getSupplier() == null ? null : p.getSupplier().getName(), p.getPurchasePrice(), p.getRetailPrice(),
                p.getQuantity(), p.getMinStock(), detection.criticalThreshold(p.getMinStock()), p.getUnit(),
                p.getReplenishmentPolicy(), status, p.stockValue(), p.isActive(), p.getUpdatedAt());
    }

    static TransactionDto toDto(InventoryTransaction t) {
        return new TransactionDto(t.getId(), t.getProduct().getId(), t.getProduct().getSku(), t.getProduct().getName(),
                t.getMovementType(), t.getQuantityChange(), t.getQuantityBefore(), t.getQuantityAfter(), t.getReason(),
                t.getPerformedBy(), t.getCreatedAt());
    }

    static StockAlertDto toDto(StockAlert a) {
        Product p = a.getProduct();
        return new StockAlertDto(a.getId(), p.getId(), p.getSku(), p.getName(), a.getQuantityAtAlert(), a.getThreshold(),
                p.getQuantity(), a.getSuggestedQuantity(), a.getStrategy(), a.getStatus().name(), a.getDocumentId(),
                a.getCreatedAt(), a.getResolvedAt());
    }
}
