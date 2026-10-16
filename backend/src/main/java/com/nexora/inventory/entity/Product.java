package com.nexora.inventory.entity;

import com.nexora.shared.exception.BusinessRuleException;
import com.nexora.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String sku;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "purchase_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "retail_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal retailPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "min_stock", nullable = false)
    private int minStock;

    @Column(nullable = false, length = 20)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "replenishment_strategy", nullable = false, length = 30)
    private ReplenishmentPolicy replenishmentPolicy;

    @Column(nullable = false)
    private boolean active = true;

    @Version
    private long version;

    protected Product() {
    }

    public Product(String sku, String name, Category category) {
        this.sku = sku;
        this.name = name;
        this.category = category;
    }

    /** Applies a signed movement; the only way the quantity changes. */
    public void applyMovement(int delta) {
        long next = (long) quantity + delta;
        if (next < 0) {
            throw new BusinessRuleException("INSUFFICIENT_STOCK",
                    "«" + name + "» тауарынан " + (-delta) + " " + unit + " шығару мүмкін емес: қоймада тек " + quantity + " " + unit);
        }
        if (next > 1_000_000) {
            throw new BusinessRuleException("STOCK_LIMIT", "Қор 1 000 000 бірліктен аспауы керек");
        }
        quantity = (int) next;
    }

    public BigDecimal stockValue() {
        return purchasePrice.multiply(BigDecimal.valueOf(quantity));
    }

    public void update(String name, String description, Category category, Supplier supplier, BigDecimal purchasePrice,
                       BigDecimal retailPrice, int minStock, String unit, ReplenishmentPolicy policy) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.supplier = supplier;
        this.purchasePrice = purchasePrice;
        this.retailPrice = retailPrice;
        this.minStock = minStock;
        this.unit = unit;
        this.replenishmentPolicy = policy;
    }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public Supplier getSupplier() { return supplier; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public BigDecimal getRetailPrice() { return retailPrice; }
    public int getQuantity() { return quantity; }
    public int getMinStock() { return minStock; }
    public String getUnit() { return unit; }
    public ReplenishmentPolicy getReplenishmentPolicy() { return replenishmentPolicy; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
