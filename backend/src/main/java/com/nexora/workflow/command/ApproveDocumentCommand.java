package com.nexora.workflow.command;

import com.nexora.shared.command.Command;
import com.nexora.shared.security.AuthenticatedUser;
import com.nexora.workflow.dto.WorkflowDtos.WorkflowOutcome;
import com.nexora.workflow.facade.DocumentWorkflowFacade;

public final class ApproveDocumentCommand implements Command<WorkflowOutcome> {

    private final DocumentWorkflowFacade receiver;
    private final Long documentId;
    private final String comment;
    private final AuthenticatedUser user;

    public ApproveDocumentCommand(DocumentWorkflowFacade receiver, Long documentId, String comment, AuthenticatedUser user) {
        this.receiver = receiver;
        this.documentId = documentId;
        this.comment = comment;
        this.user = user;
    }

    @Override public String name() { return "APPROVE_DOCUMENT"; }
    @Override public String entityType() { return "Document"; }
    @Override public WorkflowOutcome execute() { return receiver.approve(documentId, comment, user); }
    @Override public String entityId(WorkflowOutcome r) { return String.valueOf(documentId); }
    @Override public String details() { return comment == null ? "" : comment; }
}
