package com.nexora.workflow.state;

import com.nexora.workflow.entity.Document;
import com.nexora.workflow.entity.DocumentStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

/** Submitted and waiting for the first approver. */
@Component
public class PendingApprovalState implements DocumentState {

    @Override public DocumentStatus status() { return DocumentStatus.PENDING_APPROVAL; }

    @Override public Set<String> allowedActions() { return Set.of("approve", "reject", "return"); }

    @Override
    public void approve(Document document, boolean lastStep, Instant now) {
        document.moveTo(lastStep ? DocumentStatus.APPROVED : DocumentStatus.IN_REVIEW, now);
    }

    @Override
    public void reject(Document document, String reason, Instant now) {
        document.markRejected(reason, now);
    }

    @Override
    public void returnForRevision(Document document, Instant now) {
        document.moveTo(DocumentStatus.DRAFT, now);
    }
}
