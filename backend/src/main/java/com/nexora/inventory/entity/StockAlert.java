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

@Entity
@Table(name = "stock_alerts")
public class StockAlert {

    public enum Status { OPEN, REQUEST_CREATED, RESOLVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "quantity_at_alert", nullable = false)
    private int quantityAtAlert;

    @Column(nullable = false)
    private int threshold;

    @Column(name = "suggested_quantity", nullable = false)
    private int suggestedQuantity;

    @Column(nullable = false, length = 30)
    private String strategy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.OPEN;

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected StockAlert() {
    }

    public StockAlert(Product product, int quantityAtAlert, int threshold, int suggestedQuantity, String strategy,
                      Instant createdAt) {
        this.product = product;
        this.quantityAtAlert = quantityAtAlert;
        this.threshold = threshold;
        this.suggestedQuantity = suggestedQuantity;
        this.strategy = strategy;
        this.createdAt = createdAt;
    }

    public void linkReplenishmentRequest(Long documentId) {
        this.documentId = documentId;
        this.status = Status.REQUEST_CREATED;
    }

    public void resolve(Instant at) {
        this.status = Status.RESOLVED;
        this.resolvedAt = at;
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public int getQuantityAtAlert() { return quantityAtAlert; }
    public int getThreshold() { return threshold; }
    public int getSuggestedQuantity() { return suggestedQuantity; }
    public String getStrategy() { return strategy; }
    public Status getStatus() { return status; }
    public Long getDocumentId() { return documentId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
}
