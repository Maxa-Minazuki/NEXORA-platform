package com.nexora.workflow.command;

import com.nexora.shared.command.Command;
import com.nexora.shared.security.AuthenticatedUser;
import com.nexora.workflow.dto.WorkflowDtos.WorkflowOutcome;
import com.nexora.workflow.facade.DocumentWorkflowFacade;

public final class RejectDocumentCommand implements Command<WorkflowOutcome> {

    private final DocumentWorkflowFacade receiver;
    private final Long documentId;
    private final String reason;
    private final AuthenticatedUser user;

    public RejectDocumentCommand(DocumentWorkflowFacade receiver, Long documentId, String reason, AuthenticatedUser user) {
        this.receiver = receiver;
        this.documentId = documentId;
        this.reason = reason;
        this.user = user;
    }

    @Override public String name() { return "REJECT_DOCUMENT"; }
    @Override public String entityType() { return "Document"; }
    @Override public WorkflowOutcome execute() { return receiver.reject(documentId, reason, user); }
    @Override public String entityId(WorkflowOutcome r) { return String.valueOf(documentId); }
    @Override public String details() { return "Себебі: " + reason; }
}
