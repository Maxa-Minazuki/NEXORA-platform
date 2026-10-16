package com.nexora.workflow.facade;

import com.nexora.inventory.entity.StockAlert;
import com.nexora.inventory.service.InventoryService;
import com.nexora.shared.exception.BusinessRuleException;
import com.nexora.shared.exception.ConflictException;
import com.nexora.shared.exception.NotFoundException;
import com.nexora.shared.security.AuthenticatedUser;
import com.nexora.shared.security.Role;
import com.nexora.workflow.chain.DocumentProcessingChain;
import com.nexora.workflow.chain.DocumentReview;
import com.nexora.workflow.dto.WorkflowDtos.ChainResult;
import com.nexora.workflow.dto.WorkflowDtos.CreateDocumentRequest;
import com.nexora.workflow.dto.WorkflowDtos.DocumentDetail;
import com.nexora.workflow.dto.WorkflowDtos.UpdateDocumentRequest;
import com.nexora.workflow.dto.WorkflowDtos.WorkflowOutcome;
import com.nexora.workflow.entity.Document;
import com.nexora.workflow.entity.DocumentAction;
import com.nexora.workflow.entity.DocumentComment;
import com.nexora.workflow.entity.DocumentRevision;
import com.nexora.workflow.entity.DocumentStatus;
import com.nexora.workflow.entity.DocumentType;
import com.nexora.workflow.mediator.WorkflowAction;
import com.nexora.workflow.mediator.WorkflowContext;
import com.nexora.workflow.mediator.WorkflowMediator;
import com.nexora.workflow.repository.DocumentActionRepository;
import com.nexora.workflow.repository.DocumentCommentRepository;
import com.nexora.workflow.repository.DocumentRepository;
import com.nexora.workflow.repository.DocumentRevisionRepository;
import com.nexora.workflow.service.DocumentAccessPolicy;
import com.nexora.workflow.service.DocumentViewService;
import com.nexora.workflow.state.DocumentStateRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

/**
 * <h2>Design pattern: Facade</h2>
 *
 * Simple entry points for the document workflow (create, edit draft, submit, approve, reject, return,
 * comment, replenishment-from-alert, system documents). The facade loads and locks the document,
 * checks object-level access and hands the interaction to the {@link WorkflowMediator}; the
 * individual Commands call these methods as their receiver.
 */
@Service
public class DocumentWorkflowFacade {

    private final DocumentRepository documents;
    private final DocumentActionRepository actions;
    private final DocumentCommentRepository comments;
    private final DocumentRevisionRepository revisions;
    private final DocumentStateRegistry states;
    private final DocumentProcessingChain chain;
    private final WorkflowMediator mediator;
    private final DocumentAccessPolicy access;
    private final DocumentViewService views;
    private final InventoryService inventory;
    private final Clock clock;

    public DocumentWorkflowFacade(DocumentRepository documents, DocumentActionRepository actions,
                                  DocumentCommentRepository comments, DocumentRevisionRepository revisions,
                                  DocumentStateRegistry states, DocumentProcessingChain chain,
                                  WorkflowMediator mediator, DocumentAccessPolicy access, DocumentViewService views,
                                  InventoryService inventory, Clock clock) {
        this.documents = documents;
        this.actions = actions;
        this.comments = comments;
        this.revisions = revisions;
        this.states = states;
        this.chain = chain;
        this.mediator = mediator;
        this.access = access;
        this.views = views;
        this.inventory = inventory;
        this.clock = clock;
    }

    @Transactional
    public DocumentDetail create(CreateDocumentRequest r, AuthenticatedUser author) {
        if (r.type().systemIssued()) {
            throw new BusinessRuleException("SYSTEM_DOCUMENT", "«" + r.type().label() + "» құжатын тек платформа шығарады");
        }
        if (!r.type().authorRoles().contains(author.role())) {
            throw new com.nexora.shared.exception.ForbiddenOperationException(
                    com.nexora.shared.i18n.Kk.role(author.role().name()) + " рөлі «" + r.type().label() + "» құжатын жасай алмайды");
        }
        String relatedType = r.relatedEntityId() == null ? null
                : (r.relatedEntityType() == null || r.relatedEntityType().isBlank() ? r.type().relatedEntityType() : r.relatedEntityType());
        Document d = newDocument(r.type(), r.title().trim(), r.content().trim(), r.amount(), author.id(), relatedType,
                r.relatedEntityId());
        return views.detail(d, author);
    }

    @Transactional
    public DocumentDetail updateDraft(Long id, UpdateDocumentRequest r, AuthenticatedUser user) {
        Document d = lock(id);
        access.checkCanView(user, d);
        access.checkIsAuthor(user, d);
        states.of(d).edit(d);
        d.edit(r.title().trim(), r.content().trim(), r.amount());
        documents.flush();
        revisions.save(new DocumentRevision(d, user.id(), clock.instant()));
        actions.save(new DocumentAction(d.getId(), "EDIT", user.id(), d.getStatus(), d.getStatus(),
                d.getRevision() + "-нұсқа", clock.instant()));
        return views.detail(d, user);
    }

    @Transactional
    public WorkflowOutcome submit(Long id, AuthenticatedUser user) {
        Document d = lock(id);
        access.checkCanView(user, d);
        access.checkIsAuthor(user, d);
        return run(d, WorkflowAction.SUBMIT, user, null);
    }

    @Transactional
    public WorkflowOutcome approve(Long id, String comment, AuthenticatedUser user) {
        return decide(id, WorkflowAction.APPROVE, comment, user);
    }

    @Transactional
    public WorkflowOutcome reject(Long id, String reason, AuthenticatedUser user) {
        return decide(id, WorkflowAction.REJECT, reason.trim(), user);
    }

    @Transactional
    public WorkflowOutcome returnForRevision(Long id, String comment, AuthenticatedUser user) {
        return decide(id, WorkflowAction.RETURN, comment.trim(), user);
    }

    @Transactional
    public DocumentDetail comment(Long id, String body, AuthenticatedUser user) {
        Document d = documents.findById(id).orElseThrow(() -> new NotFoundException("Document", id));
        access.checkCanView(user, d);
        comments.save(new DocumentComment(d.getId(), user.id(), body.trim(), clock.instant()));
        return views.detail(d, user);
    }

    /** Integration: low-stock alert (inventory) -> replenishment request (workflow), optionally submitted. */
    @Transactional
    public WorkflowOutcome replenishmentFromAlert(Long alertId, boolean submit, AuthenticatedUser user) {
        if (!DocumentType.INVENTORY_REPLENISHMENT.authorRoles().contains(user.role())) {
            throw new com.nexora.shared.exception.ForbiddenOperationException("Сіздің рөліңіз толықтыру өтінімін жасай алмайды");
        }
        StockAlert alert = inventory.requireAlert(alertId);
        if (alert.getStatus() != StockAlert.Status.OPEN) {
            throw new ConflictException("ALERT_NOT_OPEN", "Бұл ескерту бойынша толықтыру өтінімі бұрыннан бар");
        }
        var product = alert.getProduct();
        BigDecimal amount = product.getPurchasePrice().multiply(BigDecimal.valueOf(alert.getSuggestedQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
        String content = """
                #%d қор ескертуі негізінде жасалған толықтыру өтінімі.
                Тауар: %s (%s)
                Ағымдағы қор: %d %s, шекті деңгей: %d
                Ұсынылған тапсырыс: %d %s (стратегия: %s)
                Бірлігінің сатып алу бағасы: %s, болжамды сома: %s
                """.formatted(alert.getId(), product.getName(), product.getSku(), product.getQuantity(), product.getUnit(),
                alert.getThreshold(), alert.getSuggestedQuantity(), product.getUnit(), com.nexora.shared.i18n.Kk.replenishment(alert.getStrategy()),
                product.getPurchasePrice().toPlainString(), amount.toPlainString()).trim();
        Document d = newDocument(DocumentType.INVENTORY_REPLENISHMENT, "Толықтыру: " + product.getName(), content, amount,
                user.id(), "PRODUCT", product.getId());
        inventory.linkReplenishmentRequest(alertId, d.getId());
        if (submit) {
            return run(d, WorkflowAction.SUBMIT, user, null);
        }
        return new WorkflowOutcome(views.detail(d, user), List.of("DocumentWorkflowFacade: " + alertId + " ескертуінен қаралама жасалды"));
    }

    /** Platform-issued document (e.g. flight confirmation): goes through the same chain and State. */
    @Transactional
    public Document issueSystemDocument(DocumentType type, String title, String content, Long authorId,
                                        String relatedType, Long relatedId) {
        Document d = newDocument(type, title, content, null, authorId, relatedType, relatedId);
        mediator.run(new WorkflowContext(d, WorkflowAction.SUBMIT, null, Role.USER, null, true, clock.instant()));
        return d;
    }

    /** Lab / preview: run the Chain of Responsibility without persisting anything. */
    public ChainResult dryRun(CreateDocumentRequest r, Role authorRole) {
        DocumentReview review = chain.review(new DocumentReview(r.type(), r.title(), r.content(), r.amount(),
                r.relatedEntityId() == null ? null : (r.relatedEntityType() == null ? r.type().relatedEntityType() : r.relatedEntityType()),
                r.relatedEntityId(), authorRole, false));
        return new ChainResult(!review.stopped(), review.failure(), review.decision(), review.trace(), review.steps(),
                chain.handlerNames());
    }

    private WorkflowOutcome decide(Long id, WorkflowAction action, String comment, AuthenticatedUser user) {
        Document d = lock(id);
        access.checkCanView(user, d);
        return run(d, action, user, comment);
    }

    private WorkflowOutcome run(Document d, WorkflowAction action, AuthenticatedUser user, String comment) {
        WorkflowContext ctx = mediator.run(new WorkflowContext(d, action, user.id(), user.role(), comment, false,
                clock.instant()));
        documents.flush();
        return new WorkflowOutcome(views.detail(d, user), ctx.log());
    }

    private Document newDocument(DocumentType type, String title, String content, BigDecimal amount, Long authorId,
                                 String relatedType, Long relatedId) {
        Instant now = clock.instant();
        Document d = documents.saveAndFlush(new Document(type, title, content, amount, authorId, relatedType, relatedId));
        d.assignNumber(now.atZone(ZoneOffset.UTC).getYear());
        revisions.save(new DocumentRevision(d, authorId, now));
        actions.save(new DocumentAction(d.getId(), "CREATE", authorId, null, DocumentStatus.DRAFT,
                type.label(), now));
        return d;
    }

    private Document lock(Long id) {
        return documents.lockById(id).orElseThrow(() -> new NotFoundException("Document", id));
    }
}
