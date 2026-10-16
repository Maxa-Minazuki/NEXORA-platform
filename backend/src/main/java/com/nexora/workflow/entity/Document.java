package com.nexora.workflow.entity;

import com.nexora.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

/** State-pattern context for the document workflow; status changes only through {@link #moveTo}. */
@Entity
@Table(name = "documents")
public class Document extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 20)
    private String number;

    @Column(nullable = false, length = 160)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 40)
    private DocumentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentStatus status = DocumentStatus.DRAFT;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(name = "related_entity_type", length = 40)
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    @Column(name = "approval_round", nullable = false)
    private int approvalRound;

    @Column(nullable = false)
    private int revision = 1;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Version
    private long version;

    protected Document() {
    }

    public Document(DocumentType type, String title, String content, BigDecimal amount, Long authorId,
                    String relatedEntityType, Long relatedEntityId) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.amount = amount;
        this.authorId = authorId;
        this.relatedEntityType = relatedEntityType;
        this.relatedEntityId = relatedEntityId;
    }

    public void assignNumber(int year) {
        this.number = "DOC-" + year + "-" + String.format("%05d", id);
    }

    /** Draft editing; a new revision number is recorded by the caller. */
    public void edit(String title, String content, BigDecimal amount) {
        this.title = title;
        this.content = content;
        this.amount = amount;
        this.revision++;
    }

    /** Transition hook for State objects only. */
    public void moveTo(DocumentStatus next, Instant at) {
        switch (next) {
            case PENDING_APPROVAL -> {
                this.approvalRound++;
                this.submittedAt = at;
                this.rejectionReason = null;
                this.completedAt = null;
            }
            case APPROVED -> this.completedAt = at;
            default -> {
            }
        }
        this.status = next;
    }

    /** Auto-approval of a document whose route has no steps (still a new round). */
    public void autoApprove(Instant at) {
        this.approvalRound++;
        this.submittedAt = at;
        this.completedAt = at;
        this.status = DocumentStatus.APPROVED;
    }

    public void markRejected(String reason, Instant at) {
        this.rejectionReason = reason;
        this.completedAt = at;
        this.status = DocumentStatus.REJECTED;
    }

    public Long getId() { return id; }
    public String getNumber() { return number; }
    public String getTitle() { return title; }
    public DocumentType getType() { return type; }
    public DocumentStatus getStatus() { return status; }
    public Long getAuthorId() { return authorId; }
    public BigDecimal getAmount() { return amount; }
    public String getContent() { return content; }
    public String getRelatedEntityType() { return relatedEntityType; }
    public Long getRelatedEntityId() { return relatedEntityId; }
    public int getApprovalRound() { return approvalRound; }
    public int getRevision() { return revision; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public String getRejectionReason() { return rejectionReason; }
}
