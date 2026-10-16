package com.nexora.workflow.mediator;

import org.springframework.stereotype.Component;

/**
 * Concrete workflow mediator. Interaction rules:
 * <ul>
 *   <li>SUBMIT: state precheck -> plan route (chain) -> change state -> open steps -> history -> event</li>
 *   <li>APPROVE / REJECT / RETURN: state precheck -> decide step -> change state -> history -> event</li>
 * </ul>
 */
@Component
public class DocumentWorkflowMediator implements WorkflowMediator {

    private final ApprovalRoutePlanner planner;
    private final ApprovalStepManager stepManager;
    private final DocumentLifecycleKeeper lifecycle;
    private final WorkflowHistoryRecorder history;
    private final WorkflowEventRelay relay;

    public DocumentWorkflowMediator(ApprovalRoutePlanner planner, ApprovalStepManager stepManager,
                                    DocumentLifecycleKeeper lifecycle, WorkflowHistoryRecorder history,
                                    WorkflowEventRelay relay) {
        this.planner = planner;
        this.stepManager = stepManager;
        this.lifecycle = lifecycle;
        this.history = history;
        this.relay = relay;
        for (WorkflowColleague c : new WorkflowColleague[]{planner, stepManager, lifecycle, history, relay}) {
            c.attach(this);
        }
    }

    @Override
    public WorkflowContext run(WorkflowContext ctx) {
        lifecycle.precheck(ctx);
        notify(null, ctx.action == WorkflowAction.SUBMIT ? WorkflowSignal.SUBMIT_REQUESTED : WorkflowSignal.DECISION_REQUESTED, ctx);
        return ctx;
    }

    @Override
    public void notify(WorkflowColleague sender, WorkflowSignal signal, WorkflowContext ctx) {
        ctx.log("Mediator <- " + (sender == null ? "DocumentWorkflowFacade" : sender.role()) + ": " + signal);
        boolean submit = ctx.action == WorkflowAction.SUBMIT;
        switch (signal) {
            case SUBMIT_REQUESTED -> planner.plan(ctx);
            case ROUTE_PLANNED -> lifecycle.apply(ctx);
            case DECISION_REQUESTED -> stepManager.decide(ctx);
            case STATE_CHANGED -> {
                if (submit && !ctx.review.steps().isEmpty()) {
                    stepManager.openRound(ctx);
                } else {
                    history.record(ctx);
                }
            }
            case STEPS_UPDATED -> {
                if (submit) {
                    history.record(ctx);
                } else {
                    lifecycle.apply(ctx);
                }
            }
            case HISTORY_RECORDED -> relay.publish(ctx);
            case PUBLISHED -> ctx.log("Mediator: өзара әрекет аяқталды");
        }
    }
}
