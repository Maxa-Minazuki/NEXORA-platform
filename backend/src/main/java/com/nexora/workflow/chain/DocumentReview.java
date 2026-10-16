package com.nexora.workflow.chain;

import com.nexora.shared.security.Role;
import com.nexora.workflow.entity.DocumentType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * The request object travelling along the Chain of Responsibility: the document facts to check,
 * plus the results accumulated by the handlers (trace, planned approval steps, stop reason).
 */
public final class DocumentReview {

    public enum Outcome { PASSED, STEP_ADDED, SKIPPED, FAILED, DECIDED }

    public record TraceEntry(String handler, Outcome outcome, String message) {
    }

    public record PlannedStep(String name, Role requiredRole, String reason) {
    }

    private final DocumentType type;
    private final String title;
    private final String content;
    private final BigDecimal amount;
    private final String relatedEntityType;
    private final Long relatedEntityId;
    private final Role authorRole;
    private final boolean systemIssued;

    private final List<TraceEntry> trace = new ArrayList<>();
    private final List<PlannedStep> steps = new ArrayList<>();
    private boolean stopped;
    private String failure;
    private String decision;

    public DocumentReview(DocumentType type, String title, String content, BigDecimal amount, String relatedEntityType,
                          Long relatedEntityId, Role authorRole, boolean systemIssued) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.amount = amount;
        this.relatedEntityType = relatedEntityType;
        this.relatedEntityId = relatedEntityId;
        this.authorRole = authorRole;
        this.systemIssued = systemIssued;
    }

    void record(String handler, Outcome outcome, String message) {
        trace.add(new TraceEntry(handler, outcome, message));
    }

    void stop(String handler, String reason) {
        record(handler, Outcome.FAILED, reason);
        this.stopped = true;
        this.failure = reason;
    }

    void addStep(String handler, PlannedStep step) {
        steps.add(step);
        record(handler, Outcome.STEP_ADDED, step.name() + " (" + com.nexora.shared.i18n.Kk.role(step.requiredRole().name()) + "): " + step.reason());
    }

    void decide(String handler, String decision) {
        this.decision = decision;
        record(handler, Outcome.DECIDED, decision);
    }

    public DocumentType type() { return type; }
    public String title() { return title; }
    public String content() { return content; }
    public BigDecimal amount() { return amount; }
    public String relatedEntityType() { return relatedEntityType; }
    public Long relatedEntityId() { return relatedEntityId; }
    public Role authorRole() { return authorRole; }
    public boolean systemIssued() { return systemIssued; }
    public List<TraceEntry> trace() { return List.copyOf(trace); }
    public List<PlannedStep> steps() { return List.copyOf(steps); }
    public boolean stopped() { return stopped; }
    public String failure() { return failure; }
    public String decision() { return decision; }
    public boolean hasStepFor(Role role) { return steps.stream().anyMatch(s -> s.requiredRole() == role); }
}
