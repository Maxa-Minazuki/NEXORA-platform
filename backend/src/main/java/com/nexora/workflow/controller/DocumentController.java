package com.nexora.workflow.controller;

import com.nexora.shared.command.CommandInvoker;
import com.nexora.shared.command.SimpleCommand;
import com.nexora.shared.security.AuthenticatedUser;
import com.nexora.shared.security.CurrentUser;
import com.nexora.shared.web.PageResponse;
import com.nexora.workflow.command.ApproveDocumentCommand;
import com.nexora.workflow.command.CreateDocumentCommand;
import com.nexora.workflow.command.RejectDocumentCommand;
import com.nexora.workflow.command.ReturnDocumentCommand;
import com.nexora.workflow.command.SubmitDocumentCommand;
import com.nexora.workflow.dto.WorkflowDtos.ChainResult;
import com.nexora.workflow.dto.WorkflowDtos.CommentRequest;
import com.nexora.workflow.dto.WorkflowDtos.CreateDocumentRequest;
import com.nexora.workflow.dto.WorkflowDtos.DecisionRequest;
import com.nexora.workflow.dto.WorkflowDtos.DocumentDetail;
import com.nexora.workflow.dto.WorkflowDtos.DocumentSummary;
import com.nexora.workflow.dto.WorkflowDtos.RejectRequest;
import com.nexora.workflow.dto.WorkflowDtos.ReplenishmentFromAlertRequest;
import com.nexora.workflow.dto.WorkflowDtos.ReturnRequest;
import com.nexora.workflow.dto.WorkflowDtos.TypeInfo;
import com.nexora.workflow.dto.WorkflowDtos.UpdateDocumentRequest;
import com.nexora.workflow.dto.WorkflowDtos.WorkflowOutcome;
import com.nexora.workflow.entity.DocumentStatus;
import com.nexora.workflow.entity.DocumentType;
import com.nexora.workflow.facade.DocumentWorkflowFacade;
import com.nexora.workflow.service.DocumentViewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@RestController
public class DocumentController {

    private final DocumentWorkflowFacade facade;
    private final DocumentViewService views;
    private final CommandInvoker invoker;

    public DocumentController(DocumentWorkflowFacade facade, DocumentViewService views, CommandInvoker invoker) {
        this.facade = facade;
        this.views = views;
        this.invoker = invoker;
    }

    @GetMapping("/api/documents/types")
    public List<TypeInfo> types() {
        return Arrays.stream(DocumentType.values()).map(t -> new TypeInfo(t, t.label(), t.amountRequired(),
                t.relatedEntityType(), t.systemIssued(), t.authorRoles())).toList();
    }

    @GetMapping("/api/documents")
    public PageResponse<DocumentSummary> search(@RequestParam(required = false) Long authorId,
                                                @RequestParam(defaultValue = "false") boolean mine,
                                                @RequestParam(required = false) DocumentType type,
                                                @RequestParam(required = false) DocumentStatus status,
                                                @RequestParam(required = false) Instant from,
                                                @RequestParam(required = false) Instant to,
                                                @RequestParam(required = false) String q,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) String sort) {
        return views.search(new DocumentViewService.DocumentFilter(authorId, mine, type, status, from, to, q),
                CurrentUser.require(), page, size, sort);
    }

    @GetMapping("/api/documents/{id}")
    public DocumentDetail detail(@PathVariable Long id) {
        return views.detail(id, CurrentUser.require());
    }

    @PostMapping("/api/documents")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE','DOCTOR')")
    public DocumentDetail create(@Valid @RequestBody CreateDocumentRequest request) {
        return invoker.run(new CreateDocumentCommand(facade, request, CurrentUser.require()));
    }

    @PutMapping("/api/documents/{id}")
    public DocumentDetail update(@PathVariable Long id, @Valid @RequestBody UpdateDocumentRequest request) {
        AuthenticatedUser user = CurrentUser.require();
        return invoker.run(SimpleCommand.of("EDIT_DOCUMENT", "Document", () -> facade.updateDraft(id, request, user),
                d -> d.summary().id(), request.title()));
    }

    @PostMapping("/api/documents/{id}/submit")
    public WorkflowOutcome submit(@PathVariable Long id) {
        return invoker.run(new SubmitDocumentCommand(facade, id, CurrentUser.require()));
    }

    @PostMapping("/api/documents/{id}/comments")
    public DocumentDetail comment(@PathVariable Long id, @Valid @RequestBody CommentRequest request) {
        AuthenticatedUser user = CurrentUser.require();
        return invoker.run(SimpleCommand.of("COMMENT_DOCUMENT", "Document", () -> facade.comment(id, request.body(), user),
                d -> d.summary().id(), "Пікір қосылды"));
    }

    // ------------------------------------------------------------------ workflow decisions
    @GetMapping("/api/workflows/inbox")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<DocumentSummary> inbox() {
        return views.awaitingMyAction(CurrentUser.require());
    }

    @PostMapping("/api/workflows/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public WorkflowOutcome approve(@PathVariable Long id, @Valid @RequestBody(required = false) DecisionRequest request) {
        String comment = request == null ? null : request.comment();
        return invoker.run(new ApproveDocumentCommand(facade, id, comment, CurrentUser.require()));
    }

    @PostMapping("/api/workflows/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public WorkflowOutcome reject(@PathVariable Long id, @Valid @RequestBody RejectRequest request) {
        return invoker.run(new RejectDocumentCommand(facade, id, request.reason(), CurrentUser.require()));
    }

    @PostMapping("/api/workflows/{id}/return")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public WorkflowOutcome returnForRevision(@PathVariable Long id, @Valid @RequestBody ReturnRequest request) {
        return invoker.run(new ReturnDocumentCommand(facade, id, request.comment(), CurrentUser.require()));
    }

    @PostMapping("/api/workflows/validate")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE','DOCTOR')")
    public ChainResult validate(@Valid @RequestBody CreateDocumentRequest request) {
        return facade.dryRun(request, CurrentUser.require().role());
    }

    @PostMapping("/api/workflows/replenishment-from-alert/{alertId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public WorkflowOutcome replenishmentFromAlert(@PathVariable Long alertId,
                                                  @RequestBody(required = false) ReplenishmentFromAlertRequest request) {
        AuthenticatedUser user = CurrentUser.require();
        boolean submit = request != null && request.submit();
        return invoker.run(SimpleCommand.of("CREATE_REPLENISHMENT_REQUEST", "Document",
                () -> facade.replenishmentFromAlert(alertId, submit, user), o -> o.document().summary().id(),
                "Қор ескертуінен: " + alertId + (submit ? " (жіберілді)" : "")));
    }
}
