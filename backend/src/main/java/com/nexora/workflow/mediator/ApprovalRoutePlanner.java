package com.nexora.workflow.mediator;

import com.nexora.shared.exception.BusinessRuleException;
import com.nexora.workflow.chain.DocumentProcessingChain;
import com.nexora.workflow.chain.DocumentReview;
import com.nexora.workflow.entity.Document;
import org.springframework.stereotype.Component;

/** Colleague: runs the Chain of Responsibility and turns its result into an approval route. */
@Component
public class ApprovalRoutePlanner extends WorkflowColleague {

    private final DocumentProcessingChain chain;

    public ApprovalRoutePlanner(DocumentProcessingChain chain) {
        this.chain = chain;
    }

    @Override public String role() { return "ApprovalRoutePlanner"; }

    void plan(WorkflowContext ctx) {
        Document d = ctx.document;
        DocumentReview review = chain.review(new DocumentReview(d.getType(), d.getTitle(), d.getContent(), d.getAmount(),
                d.getRelatedEntityType(), d.getRelatedEntityId(), ctx.actorRole, ctx.systemIssued));
        ctx.review = review;
        if (review.stopped()) {
            ctx.log(role() + ": тізбек тоқтады - " + review.failure());
            throw new BusinessRuleException("VALIDATION_CHAIN_FAILED", "Құжат тексеруден өтпеді: " + review.failure());
        }
        ctx.log(role() + ": тізбектен өтті, бекіту қадамдары: " + review.steps().size());
        report(WorkflowSignal.ROUTE_PLANNED, ctx);
    }
}
