package com.nexora.workflow.mediator;

/**
 * <h2>Design pattern: Mediator</h2>
 *
 * Coordinates the participants of an approval: route planner (runs the Chain of Responsibility),
 * step manager (approval steps, approver eligibility), lifecycle keeper (State pattern), history
 * recorder and event relay (Observer). None of them references another.
 */
public interface WorkflowMediator {

    void notify(WorkflowColleague sender, WorkflowSignal signal, WorkflowContext ctx);

    WorkflowContext run(WorkflowContext ctx);
}
