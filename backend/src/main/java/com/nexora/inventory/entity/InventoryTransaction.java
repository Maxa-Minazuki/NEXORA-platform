package com.nexora.inventory.entity;

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

import java.time.Instant;

/** Immutable ledger line describing one stock movement. */
@Entity
@Table(name = "inventory_transactions")
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private MovementType movementType;

    @Column(name = "quantity_change", nullable = false)
    private int quantityChange;

    @Column(name = "quantity_before", nullable = false)
    private int quantityBefore;

    @Column(name = "quantity_after", nullable = false)
    private int quantityAfter;

    @Column(length = 255)
    private String reason;

    @Column(name = "performed_by")
    private Long performedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected InventoryTransaction() {
    }

    public InventoryTransaction(Product product, MovementType type, int change, int before, int after, String reason,
                                Long performedBy, Instant createdAt) {
        this.product = product;
        this.movementType = type;
        this.quantityChange = change;
        this.quantityBefore = before;
        this.quantityAfter = after;
        this.reason = reason;
        this.performedBy = performedBy;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public MovementType getMovementType() { return movementType; }
    public int getQuantityChange() { return quantityChange; }
    public int getQuantityBefore() { return quantityBefore; }
    public int getQuantityAfter() { return quantityAfter; }
    public String getReason() { return reason; }
    public Long getPerformedBy() { return performedBy; }
    public Instant getCreatedAt() { return createdAt; }
}
