package com.nexora.workflow.command;

import com.nexora.shared.command.Command;
import com.nexora.shared.security.AuthenticatedUser;
import com.nexora.workflow.dto.WorkflowDtos.WorkflowOutcome;
import com.nexora.workflow.facade.DocumentWorkflowFacade;

public final class SubmitDocumentCommand implements Command<WorkflowOutcome> {

    private final DocumentWorkflowFacade receiver;
    private final Long documentId;
    private final AuthenticatedUser user;

    public SubmitDocumentCommand(DocumentWorkflowFacade receiver, Long documentId, AuthenticatedUser user) {
        this.receiver = receiver;
        this.documentId = documentId;
        this.user = user;
    }

    @Override public String name() { return "SUBMIT_DOCUMENT"; }
    @Override public String entityType() { return "Document"; }
    @Override public WorkflowOutcome execute() { return receiver.submit(documentId, user); }
    @Override public String entityId(WorkflowOutcome r) { return String.valueOf(documentId); }
    @Override public String details() { return "Бекітуге жіберілді"; }
}
