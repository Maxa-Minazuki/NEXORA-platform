package com.nexora.workflow.mediator;

import com.nexora.shared.security.Role;
import com.nexora.workflow.chain.DocumentReview;
import com.nexora.workflow.entity.ApprovalStep;
import com.nexora.workflow.entity.Document;
import com.nexora.workflow.entity.DocumentStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Per-request working data for one workflow interaction; never shared between requests. */
public final class WorkflowContext {

    final Document document;
    final WorkflowAction action;
    final Long actorId;
    final Role actorRole;
    final String comment;
    final boolean systemIssued;
    final Instant now;
    final DocumentStatus statusBefore;

    DocumentReview review;
    ApprovalStep decidedStep;
    ApprovalStep nextStep;
    boolean lastStep;
    private final List<String> log = new ArrayList<>();

    public WorkflowContext(Document document, WorkflowAction action, Long actorId, Role actorRole, String comment,
                           boolean systemIssued, Instant now) {
        this.document = document;
        this.action = action;
        this.actorId = actorId;
        this.actorRole = actorRole;
        this.comment = comment;
        this.systemIssued = systemIssued;
        this.now = now;
        this.statusBefore = document.getStatus();
    }

    void log(String line) { log.add(line); }

    public List<String> log() { return List.copyOf(log); }
    public Document document() { return document; }
    public DocumentReview review() { return review; }
}
