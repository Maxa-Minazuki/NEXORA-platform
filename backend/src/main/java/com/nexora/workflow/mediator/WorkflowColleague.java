package com.nexora.workflow.mediator;

/** Mediator colleague base: reports to the mediator, never calls other colleagues directly. */
public abstract class WorkflowColleague {

    private WorkflowMediator mediator;

    void attach(WorkflowMediator mediator) {
        this.mediator = mediator;
    }

    protected void report(WorkflowSignal signal, WorkflowContext ctx) {
        mediator.notify(this, signal, ctx);
    }

    public abstract String role();
}
