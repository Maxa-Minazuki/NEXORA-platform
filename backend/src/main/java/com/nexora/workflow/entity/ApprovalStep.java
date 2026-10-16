package com.nexora.workflow.entity;

import com.nexora.shared.security.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "approval_steps")
public class ApprovalStep {

    public enum Status { WAITING, ACTIVE, APPROVED, REJECTED, RETURNED, SKIPPED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "approval_round", nullable = false)
    private int approvalRound;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(nullable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_role", nullable = false, length = 20)
    private Role requiredRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "decided_by")
    private Long decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decision_comment", length = 500)
    private String decisionComment;

    protected ApprovalStep() {
    }

    public ApprovalStep(Long documentId, int round, int order, String name, Role requiredRole, Status status) {
        this.documentId = documentId;
        this.approvalRound = round;
        this.stepOrder = order;
        this.name = name;
        this.requiredRole = requiredRole;
        this.status = status;
    }

    public void decide(Status outcome, Long userId, String comment, Instant at) {
        this.status = outcome;
        this.decidedBy = userId;
        this.decisionComment = comment;
        this.decidedAt = at;
    }

    public void activate() { this.status = Status.ACTIVE; }
    public void skip() { this.status = Status.SKIPPED; }

    public Long getId() { return id; }
    public Long getDocumentId() { return documentId; }
    public int getApprovalRound() { return approvalRound; }
    public int getStepOrder() { return stepOrder; }
    public String getName() { return name; }
    public Role getRequiredRole() { return requiredRole; }
    public Status getStatus() { return status; }
    public Long getDecidedBy() { return decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
    public String getDecisionComment() { return decisionComment; }
}
