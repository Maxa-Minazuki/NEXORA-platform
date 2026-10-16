package com.nexora.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_username", nullable = false, length = 50)
    private String actorUsername;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 60)
    private String entityType;

    @Column(name = "entity_id", length = 60)
    private String entityId;

    @Column(nullable = false, length = 10)
    private String outcome;

    @Column(length = 1000)
    private String details;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditLog() {
    }

    public AuditLog(Long actorId, String actorUsername, String action, String entityType, String entityId,
                    String outcome, String details, Instant createdAt) {
        this.actorId = actorId;
        this.actorUsername = actorUsername;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.outcome = outcome;
        this.details = details;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getActorId() { return actorId; }
    public String getActorUsername() { return actorUsername; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getOutcome() { return outcome; }
    public String getDetails() { return details; }
    public Instant getCreatedAt() { return createdAt; }
}
