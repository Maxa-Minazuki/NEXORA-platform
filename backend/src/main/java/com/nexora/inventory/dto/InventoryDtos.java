package com.nexora.inventory.dto;

import com.nexora.inventory.entity.MovementType;
import com.nexora.inventory.entity.ReplenishmentPolicy;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class InventoryDtos {

    private InventoryDtos() {
    }

    public enum StockStatus { IN_STOCK, LOW_STOCK, OUT_OF_STOCK }

    public record ProductDto(Long id, String sku, String name, String description, Long categoryId,
                             String categoryName, Long supplierId, String supplierName, BigDecimal purchasePrice,
                             BigDecimal retailPrice, int quantity, int minStock, int criticalThreshold, String unit,
                             ReplenishmentPolicy replenishmentStrategy, StockStatus stockStatus,
                             BigDecimal stockValue, boolean active, Instant updatedAt) {
    }

    public record ProductRequest(
            @NotBlank @Size(max = 40) @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "тек латын әріптері, цифрлар және сызықша") String sku,
            @NotBlank @Size(max = 120) String name,
            @Size(max = 1000) String description,
            @NotNull Long categoryId,
            Long supplierId,
            @NotNull @DecimalMin("0.00") @DecimalMax("9999999.99") @Digits(integer = 10, fraction = 2) BigDecimal purchasePrice,
            @NotNull @DecimalMin("0.00") @DecimalMax("9999999.99") @Digits(integer = 10, fraction = 2) BigDecimal retailPrice,
            @Min(0) @Max(1_000_000) int quantity,
            @Min(0) @Max(100_000) int minStock,
            @NotBlank @Size(max = 20) String unit,
            @NotNull ReplenishmentPolicy replenishmentStrategy) {
    }

    public record AdjustStockRequest(
            @NotNull MovementType type,
            @Min(-100_000) @Max(100_000) int quantity,
            @NotBlank @Size(max = 255) String reason) {
    }

    public record TransactionDto(Long id, Long productId, String productSku, String productName, MovementType type,
                                 int quantityChange, int quantityBefore, int quantityAfter, String reason,
                                 Long performedBy, Instant createdAt) {
    }

    public record AdjustmentResult(ProductDto product, TransactionDto transaction, boolean lowStockTriggered,
                                   String detectionStrategy) {
    }

    public record StockAlertDto(Long id, Long productId, String productSku, String productName, int quantityAtAlert,
                                int threshold, int currentQuantity, int suggestedQuantity, String strategy,
                                String status, Long documentId, Instant createdAt, Instant resolvedAt) {
    }

    public record CategoryValue(String category, long products, BigDecimal value) {
    }

    public record InventoryStats(long totalProducts, long totalUnits, BigDecimal stockValue, BigDecimal retailValue,
                                 long lowStockCount, long outOfStockCount, long openAlerts,
                                 String detectionStrategy, List<CategoryValue> byCategory) {
    }

    public record CategoryDto(Long id, String name, String description) {
    }

    public record CategoryRequest(@NotBlank @Size(max = 80) String name, @Size(max = 255) String description) {
    }

    public record SupplierDto(Long id, String name, String contactEmail, String phone) {
    }

    public record SupplierRequest(@NotBlank @Size(max = 120) String name, @Email @Size(max = 120) String contactEmail,
                                  @Size(max = 40) String phone) {
    }

    public record StrategyInfo(String key, String description, boolean active) {
    }
}
