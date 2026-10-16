package com.nexora.workflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/** Immutable snapshot of a document's significant fields at each revision. */
@Entity
@Table(name = "document_revisions")
public class DocumentRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(nullable = false)
    private int revision;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "changed_by")
    private Long changedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DocumentRevision() {
    }

    public DocumentRevision(Document d, Long changedBy, Instant at) {
        this.documentId = d.getId();
        this.revision = d.getRevision();
        this.title = d.getTitle();
        this.content = d.getContent();
        this.amount = d.getAmount();
        this.changedBy = changedBy;
        this.createdAt = at;
    }

    public int getRevision() { return revision; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public BigDecimal getAmount() { return amount; }
    public Long getChangedBy() { return changedBy; }
    public Instant getCreatedAt() { return createdAt; }
}
