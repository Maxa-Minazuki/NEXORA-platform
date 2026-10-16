package com.nexora.workflow.mediator;

import com.nexora.shared.event.DocumentApprovedEvent;
import com.nexora.shared.event.DocumentRejectedEvent;
import com.nexora.shared.event.DocumentReturnedEvent;
import com.nexora.shared.event.DocumentSubmittedEvent;
import com.nexora.workflow.entity.Document;
import com.nexora.workflow.entity.DocumentStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Colleague: publishes the Observer events for notifications, dashboard and audit. */
@Component
public class WorkflowEventRelay extends WorkflowColleague {

    private final ApplicationEventPublisher events;

    public WorkflowEventRelay(ApplicationEventPublisher events) {
        this.events = events;
    }

    @Override public String role() { return "WorkflowEventRelay"; }

    void publish(WorkflowContext ctx) {
        Document d = ctx.document;
        if (ctx.systemIssued) {
            ctx.log(role() + ": жүйелік құжат, оқиға жарияланбайды (дереккөз модулі хабарлайды)");
            report(WorkflowSignal.PUBLISHED, ctx);
            return;
        }
        Object event = switch (ctx.action) {
            case SUBMIT -> d.getStatus() == DocumentStatus.APPROVED
                    ? new DocumentApprovedEvent(d.getId(), d.getNumber(), d.getTitle(), d.getAuthorId(), null, true,
                    null, null, ctx.now)
                    : new DocumentSubmittedEvent(d.getId(), d.getNumber(), d.getTitle(), d.getAuthorId(),
                    ctx.review.steps().getFirst().requiredRole().name(), ctx.review.steps().getFirst().name(), ctx.now);
            case APPROVE -> new DocumentApprovedEvent(d.getId(), d.getNumber(), d.getTitle(), d.getAuthorId(), ctx.actorId,
                    ctx.lastStep, ctx.nextStep == null ? null : ctx.nextStep.getRequiredRole().name(),
                    ctx.nextStep == null ? null : ctx.nextStep.getName(), ctx.now);
            case REJECT -> new DocumentRejectedEvent(d.getId(), d.getNumber(), d.getTitle(), d.getAuthorId(), ctx.actorId,
                    ctx.comment, ctx.now);
            case RETURN -> new DocumentReturnedEvent(d.getId(), d.getNumber(), d.getTitle(), d.getAuthorId(), ctx.actorId,
                    ctx.comment, ctx.now);
        };
        events.publishEvent(event);
        ctx.log(role() + ": " + event.getClass().getSimpleName() + " жарияланды");
        report(WorkflowSignal.PUBLISHED, ctx);
    }
}
