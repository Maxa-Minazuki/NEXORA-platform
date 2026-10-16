package com.nexora.workflow.service;

import com.nexora.auth.entity.User;
import com.nexora.auth.repository.UserRepository;
import com.nexora.shared.exception.NotFoundException;
import com.nexora.shared.security.AuthenticatedUser;
import com.nexora.workflow.chain.DocumentProcessingChain;
import com.nexora.workflow.chain.DocumentReview;
import com.nexora.workflow.dto.WorkflowDtos.ActionDto;
import com.nexora.workflow.dto.WorkflowDtos.CommentDto;
import com.nexora.workflow.dto.WorkflowDtos.DocumentDetail;
import com.nexora.workflow.dto.WorkflowDtos.DocumentSummary;
import com.nexora.workflow.dto.WorkflowDtos.RevisionDto;
import com.nexora.workflow.dto.WorkflowDtos.StepDto;
import com.nexora.workflow.dto.WorkflowDtos.TrackItem;
import com.nexora.workflow.entity.ApprovalStep;
import com.nexora.workflow.entity.Document;
import com.nexora.workflow.entity.DocumentAction;
import com.nexora.workflow.entity.DocumentStatus;
import com.nexora.workflow.entity.DocumentType;
import com.nexora.workflow.mediator.ApprovalStepManager;
import com.nexora.workflow.repository.ApprovalStepRepository;
import com.nexora.workflow.repository.DocumentActionRepository;
import com.nexora.workflow.repository.DocumentCommentRepository;
import com.nexora.workflow.repository.DocumentRepository;
import com.nexora.workflow.repository.DocumentRevisionRepository;
import com.nexora.workflow.state.DocumentStateRegistry;
import com.nexora.shared.web.PageRequests;
import com.nexora.shared.web.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Read side of the workflow: lists, details, the visual approval track and the inbox. */
@Service
public class DocumentViewService {

    private final DocumentRepository documents;
    private final ApprovalStepRepository steps;
    private final DocumentActionRepository actions;
    private final DocumentCommentRepository comments;
    private final DocumentRevisionRepository revisions;
    private final UserRepository users;
    private final DocumentStateRegistry states;
    private final DocumentProcessingChain chain;
    private final DocumentAccessPolicy access;

    public DocumentViewService(DocumentRepository documents, ApprovalStepRepository steps,
                               DocumentActionRepository actions, DocumentCommentRepository comments,
                               DocumentRevisionRepository revisions, UserRepository users,
                               DocumentStateRegistry states, DocumentProcessingChain chain,
                               DocumentAccessPolicy access) {
        this.documents = documents;
        this.steps = steps;
        this.actions = actions;
        this.comments = comments;
        this.revisions = revisions;
        this.users = users;
        this.states = states;
        this.chain = chain;
        this.access = access;
    }

    public record DocumentFilter(Long authorId, boolean mine, DocumentType type, DocumentStatus status, Instant from,
                                 Instant to, String q) {
    }

    @Transactional(readOnly = true)
    public PageResponse<DocumentSummary> search(DocumentFilter f, AuthenticatedUser user, int page, int size, String sort) {
        Long authorId = (!access.seesAll(user) || f.mine()) ? user.id() : f.authorId();
        var pageable = PageRequests.of(page, size, sort, Set.of("createdAt", "updatedAt", "title", "status", "amount"), "-updatedAt");
        String q = f.q() == null || f.q().isBlank() ? null : f.q().trim();
        var result = documents.search(authorId, f.type(), f.status(), f.from(), f.to(), q, pageable);
        Map<Long, String> names = namesOf(result.getContent().stream().map(Document::getAuthorId).toList());
        return PageResponse.of(result, d -> summary(d, names, activeStep(d)));
    }

    @Transactional(readOnly = true)
    public List<DocumentSummary> awaitingMyAction(AuthenticatedUser user) {
        Collection<com.nexora.shared.security.Role> roles = user.role() == com.nexora.shared.security.Role.ADMIN
                ? EnumSet.allOf(com.nexora.shared.security.Role.class) : EnumSet.of(user.role());
        List<Document> list = documents.awaitingAction(roles, user.id(),
                EnumSet.of(DocumentStatus.PENDING_APPROVAL, DocumentStatus.IN_REVIEW));
        Map<Long, String> names = namesOf(list.stream().map(Document::getAuthorId).toList());
        return list.stream().map(d -> summary(d, names, activeStep(d))).toList();
    }

    @Transactional(readOnly = true)
    public DocumentDetail detail(Long id, AuthenticatedUser user) {
        Document d = documents.findById(id).orElseThrow(() -> new NotFoundException("Document", id));
        access.checkCanView(user, d);
        return detail(d, user);
    }

    /** Builds the full view; must be called inside a transaction where {@code d} is managed. */
    public DocumentDetail detail(Document d, AuthenticatedUser user) {
        List<ApprovalStep> all = steps.findByDocumentIdOrderByApprovalRoundAscStepOrderAsc(d.getId());
        List<DocumentAction> history = actions.findByDocumentIdOrderByCreatedAtAscIdAsc(d.getId());
        var commentList = comments.findByDocumentIdOrderByCreatedAtAsc(d.getId());
        var revisionList = revisions.findByDocumentIdOrderByRevisionDesc(d.getId());

        Set<Long> ids = new HashSet<>();
        ids.add(d.getAuthorId());
        all.forEach(s -> { if (s.getDecidedBy() != null) ids.add(s.getDecidedBy()); });
        history.forEach(a -> { if (a.getActorId() != null) ids.add(a.getActorId()); });
        commentList.forEach(c -> ids.add(c.getAuthorId()));
        revisionList.forEach(r -> { if (r.getChangedBy() != null) ids.add(r.getChangedBy()); });
        Map<Long, String> names = namesOf(ids);

        List<ApprovalStep> current = all.stream().filter(s -> s.getApprovalRound() == d.getApprovalRound()).toList();
        ApprovalStep active = current.stream().filter(s -> s.getStatus() == ApprovalStep.Status.ACTIVE).findFirst().orElse(null);
        boolean canDecide = ApprovalStepManager.canDecide(active, user.id(), user.role(), d.getAuthorId());

        Set<String> allowed = new HashSet<>(states.of(d).allowedActions());
        boolean isAuthor = d.getAuthorId().equals(user.id());
        if (!isAuthor) {
            allowed.removeAll(Set.of("edit", "submit"));
        }
        if (!canDecide) {
            allowed.removeAll(Set.of("approve", "reject", "return"));
        }

        List<DocumentReview.PlannedStep> planned = d.getStatus() == DocumentStatus.DRAFT
                ? previewRoute(d, users.findById(d.getAuthorId()).map(User::getRole).orElse(user.role()))
                : List.of();

        return new DocumentDetail(summary(d, names, Optional.ofNullable(active)), d.getContent(), d.getRelatedEntityType(),
                d.getRelatedEntityId(), d.getRevision(), d.getApprovalRound(), d.getRejectionReason(), allowed, canDecide,
                track(d, current, history, names, planned),
                all.stream().map(s -> new StepDto(s.getId(), s.getApprovalRound(), s.getStepOrder(), s.getName(),
                        s.getRequiredRole(), s.getStatus().name(), names.get(s.getDecidedBy()), s.getDecidedAt(),
                        s.getDecisionComment())).toList(),
                planned,
                history.stream().map(a -> new ActionDto(a.getAction(), a.getActorId() == null ? "system" : names.get(a.getActorId()),
                        a.getFromStatus(), a.getToStatus(), a.getNote(), a.getCreatedAt())).toList(),
                commentList.stream().map(c -> new CommentDto(c.getId(), names.get(c.getAuthorId()), c.getBody(), c.getCreatedAt())).toList(),
                revisionList.stream().map(r -> new RevisionDto(r.getRevision(), r.getTitle(), r.getAmount(),
                        names.get(r.getChangedBy()), r.getCreatedAt())).toList());
    }

    /** Dry-run of the chain on a draft so the UI can show the route the document will take. */
    public List<DocumentReview.PlannedStep> previewRoute(Document d, com.nexora.shared.security.Role authorRole) {
        DocumentReview review = chain.review(new DocumentReview(d.getType(), d.getTitle(), d.getContent(), d.getAmount(),
                d.getRelatedEntityType(), d.getRelatedEntityId(), authorRole, d.getType().systemIssued()));
        return review.stopped() ? List.of() : review.steps();
    }

    private List<TrackItem> track(Document d, List<ApprovalStep> current, List<DocumentAction> history,
                                  Map<Long, String> names, List<DocumentReview.PlannedStep> planned) {
        List<TrackItem> items = new ArrayList<>();
        items.add(new TrackItem("created", "Жасалды", "DONE", names.get(d.getAuthorId()), d.getCreatedAt(),
                d.getRevision() + "-нұсқа"));
        boolean submitted = d.getStatus() != DocumentStatus.DRAFT;
        DocumentAction lastSubmit = history.stream().filter(a -> "SUBMIT".equals(a.getAction()))
                .reduce((a, b) -> b).orElse(null);
        items.add(new TrackItem("validation", "Тексеру", submitted ? "DONE" : "PENDING",
                submitted ? "Chain of Responsibility" : null, submitted && lastSubmit != null ? lastSubmit.getCreatedAt() : null,
                submitted && lastSubmit != null ? lastSubmit.getNote() : "Жібергенде орындалады"));
        if (submitted) {
            for (ApprovalStep s : current) {
                String state = switch (s.getStatus()) {
                    case APPROVED -> "DONE";
                    case ACTIVE -> "CURRENT";
                    case REJECTED -> "FAILED";
                    case RETURNED -> "RETURNED";
                    case SKIPPED -> "SKIPPED";
                    case WAITING -> "PENDING";
                };
                items.add(new TrackItem("step-" + s.getStepOrder(), s.getName(), state,
                        s.getDecidedBy() == null ? com.nexora.shared.i18n.Kk.role(s.getRequiredRole().name()) : names.get(s.getDecidedBy()),
                        s.getDecidedAt(), s.getDecisionComment()));
            }
        } else {
            for (int i = 0; i < planned.size(); i++) {
                var p = planned.get(i);
                items.add(new TrackItem("planned-" + (i + 1), p.name(), "PENDING", com.nexora.shared.i18n.Kk.role(p.requiredRole().name()), null, p.reason()));
            }
        }
        String finalState = switch (d.getStatus()) {
            case APPROVED -> "DONE";
            case REJECTED -> "FAILED";
            default -> "PENDING";
        };
        items.add(new TrackItem("completed", d.getStatus() == DocumentStatus.REJECTED ? "Қабылданбады" : "Аяқталды", finalState,
                null, d.getCompletedAt(), d.getStatus() == DocumentStatus.REJECTED ? d.getRejectionReason() : null));
        return items;
    }

    private Optional<ApprovalStep> activeStep(Document d) {
        if (d.getStatus() != DocumentStatus.PENDING_APPROVAL && d.getStatus() != DocumentStatus.IN_REVIEW) {
            return Optional.empty();
        }
        return steps.findByDocumentIdAndApprovalRoundOrderByStepOrderAsc(d.getId(), d.getApprovalRound()).stream()
                .filter(s -> s.getStatus() == ApprovalStep.Status.ACTIVE).findFirst();
    }

    private DocumentSummary summary(Document d, Map<Long, String> names, Optional<ApprovalStep> active) {
        return new DocumentSummary(d.getId(), d.getNumber(), d.getTitle(), d.getType(), d.getType().label(), d.getStatus(),
                d.getAuthorId(), names.get(d.getAuthorId()), d.getAmount(), active.map(ApprovalStep::getName).orElse(null),
                active.map(ApprovalStep::getRequiredRole).orElse(null), d.getCreatedAt(), d.getUpdatedAt(), d.getSubmittedAt());
    }

    private Map<Long, String> namesOf(Collection<Long> ids) {
        return users.findAllById(ids.stream().filter(java.util.Objects::nonNull).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, User::getFullName, (a, b) -> a));
    }
}
