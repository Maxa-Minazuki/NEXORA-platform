package com.nexora.workflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** History line of the document's own timeline (who did what, from which status to which). */
@Entity
@Table(name = "document_actions")
public class DocumentAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(nullable = false, length = 30)
    private String action;

    @Column(name = "actor_id")
    private Long actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private DocumentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 20)
    private DocumentStatus toStatus;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DocumentAction() {
    }

    public DocumentAction(Long documentId, String action, Long actorId, DocumentStatus from, DocumentStatus to,
                          String note, Instant createdAt) {
        this.documentId = documentId;
        this.action = action;
        this.actorId = actorId;
        this.fromStatus = from;
        this.toStatus = to;
        this.note = note;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getAction() { return action; }
    public Long getActorId() { return actorId; }
    public DocumentStatus getFromStatus() { return fromStatus; }
    public DocumentStatus getToStatus() { return toStatus; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
}
