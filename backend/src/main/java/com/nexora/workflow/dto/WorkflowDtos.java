package com.nexora.workflow.dto;

import com.nexora.shared.security.Role;
import com.nexora.workflow.chain.DocumentReview;
import com.nexora.workflow.entity.DocumentStatus;
import com.nexora.workflow.entity.DocumentType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public final class WorkflowDtos {

    private WorkflowDtos() {
    }

    public record CreateDocumentRequest(
            @NotNull DocumentType type,
            @NotBlank @Size(max = 160) String title,
            @NotBlank @Size(max = 4000) String content,
            @DecimalMin("0.00") @DecimalMax("100000000.00") @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @Size(max = 40) String relatedEntityType,
            Long relatedEntityId) {
    }

    public record UpdateDocumentRequest(
            @NotBlank @Size(max = 160) String title,
            @NotBlank @Size(max = 4000) String content,
            @DecimalMin("0.00") @DecimalMax("100000000.00") @Digits(integer = 10, fraction = 2) BigDecimal amount) {
    }

    public record DecisionRequest(@Size(max = 500) String comment) {
    }

    public record RejectRequest(@NotBlank @Size(min = 5, max = 500) String reason) {
    }

    public record ReturnRequest(@NotBlank @Size(min = 5, max = 500) String comment) {
    }

    public record CommentRequest(@NotBlank @Size(max = 1000) String body) {
    }

    public record ReplenishmentFromAlertRequest(boolean submit) {
    }

    public record DocumentSummary(Long id, String number, String title, DocumentType type, String typeLabel,
                                  DocumentStatus status, Long authorId, String authorName, BigDecimal amount,
                                  String currentStep, Role currentApproverRole, Instant createdAt, Instant updatedAt,
                                  Instant submittedAt) {
    }

    public record StepDto(Long id, int round, int order, String name, Role requiredRole, String status,
                          String decidedBy, Instant decidedAt, String comment) {
    }

    /** Visual track: Created -> Validation -> [approval steps] -> Completed, derived from real data. */
    public record TrackItem(String key, String label, String state, String actor, Instant at, String note) {
    }

    public record ActionDto(String action, String actor, DocumentStatus fromStatus, DocumentStatus toStatus,
                            String note, Instant at) {
    }

    public record CommentDto(Long id, String author, String body, Instant createdAt) {
    }

    public record RevisionDto(int revision, String title, BigDecimal amount, String changedBy, Instant createdAt) {
    }

    public record DocumentDetail(DocumentSummary summary, String content, String relatedEntityType,
                                 Long relatedEntityId, int revision, int approvalRound, String rejectionReason,
                                 Set<String> allowedActions, boolean canDecide, List<TrackItem> track,
                                 List<StepDto> steps, List<DocumentReview.PlannedStep> plannedRoute,
                                 List<ActionDto> history, List<CommentDto> comments, List<RevisionDto> revisions) {
    }

    public record ChainResult(boolean passed, String failure, String decision,
                              List<DocumentReview.TraceEntry> trace, List<DocumentReview.PlannedStep> route,
                              List<String> handlers) {
    }

    public record WorkflowOutcome(DocumentDetail document, List<String> coordinationLog) {
    }

    public record TypeInfo(DocumentType type, String label, boolean amountRequired, String relatedEntityType,
                           boolean systemIssued, Set<Role> authorRoles) {
    }
}
