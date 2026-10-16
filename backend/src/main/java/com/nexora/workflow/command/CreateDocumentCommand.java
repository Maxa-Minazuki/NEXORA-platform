package com.nexora.workflow.command;

import com.nexora.shared.command.Command;
import com.nexora.shared.security.AuthenticatedUser;
import com.nexora.workflow.dto.WorkflowDtos.CreateDocumentRequest;
import com.nexora.workflow.dto.WorkflowDtos.DocumentDetail;
import com.nexora.workflow.facade.DocumentWorkflowFacade;

public final class CreateDocumentCommand implements Command<DocumentDetail> {

    private final DocumentWorkflowFacade receiver;
    private final CreateDocumentRequest request;
    private final AuthenticatedUser user;

    public CreateDocumentCommand(DocumentWorkflowFacade receiver, CreateDocumentRequest request, AuthenticatedUser user) {
        this.receiver = receiver;
        this.request = request;
        this.user = user;
    }

    @Override public String name() { return "CREATE_DOCUMENT"; }
    @Override public String entityType() { return "Document"; }
    @Override public DocumentDetail execute() { return receiver.create(request, user); }
    @Override public String entityId(DocumentDetail r) { return r == null ? null : String.valueOf(r.summary().id()); }
    @Override public String details() { return request.type() + ": " + request.title(); }
}
