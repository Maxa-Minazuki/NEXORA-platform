package com.nexora.workflow.mediator;

import com.nexora.workflow.state.DocumentState;
import com.nexora.workflow.state.DocumentStateRegistry;
import org.springframework.stereotype.Component;

/** Colleague: applies the requested operation through the document's current State object. */
@Component
public class DocumentLifecycleKeeper extends WorkflowColleague {

    private final DocumentStateRegistry states;

    public DocumentLifecycleKeeper(DocumentStateRegistry states) {
        this.states = states;
    }

    @Override public String role() { return "DocumentLifecycleKeeper"; }

    /** Validates that the current state accepts the action before anything else happens. */
    void precheck(WorkflowContext ctx) {
        DocumentState state = states.of(ctx.document);
        String action = switch (ctx.action) {
            case SUBMIT -> "submit";
            case APPROVE -> "approve";
            case REJECT -> "reject";
            case RETURN -> "return";
        };
        if (!state.allowedActions().contains(action)) {
            throw new com.nexora.shared.exception.InvalidStateTransitionException("document", state.status().name(), action);
        }
    }

    void apply(WorkflowContext ctx) {
        DocumentState state = states.of(ctx.document);
        switch (ctx.action) {
            case SUBMIT -> state.submit(ctx.document, !ctx.review.steps().isEmpty(), ctx.now);
            case APPROVE -> state.approve(ctx.document, ctx.lastStep, ctx.now);
            case REJECT -> state.reject(ctx.document, ctx.comment, ctx.now);
            case RETURN -> state.returnForRevision(ctx.document, ctx.now);
        }
        ctx.log(role() + ": " + com.nexora.shared.i18n.Kk.status(state.status().name()) + " -> " + com.nexora.shared.i18n.Kk.status(ctx.document.getStatus().name()));
        report(WorkflowSignal.STATE_CHANGED, ctx);
    }
}
