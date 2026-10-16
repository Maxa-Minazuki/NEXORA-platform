package com.nexora.workflow.mediator;

import com.nexora.workflow.entity.DocumentAction;
import com.nexora.workflow.repository.DocumentActionRepository;
import org.springframework.stereotype.Component;

/** Colleague: writes the document's own timeline entry. */
@Component
public class WorkflowHistoryRecorder extends WorkflowColleague {

    private final DocumentActionRepository actions;

    public WorkflowHistoryRecorder(DocumentActionRepository actions) {
        this.actions = actions;
    }

    @Override public String role() { return "WorkflowHistoryRecorder"; }

    void record(WorkflowContext ctx) {
        String note = switch (ctx.action) {
            case SUBMIT -> ctx.review.decision();
            case APPROVE, RETURN, REJECT -> (ctx.decidedStep == null ? "" : ctx.decidedStep.getName() + ": ")
                    + (ctx.comment == null ? "" : ctx.comment);
        };
        actions.save(new DocumentAction(ctx.document.getId(), ctx.action.name(), ctx.actorId, ctx.statusBefore,
                ctx.document.getStatus(), truncate(note), ctx.now));
        ctx.log(role() + ": тарихқа жазба қосылды (" + ctx.action + ")");
        report(WorkflowSignal.HISTORY_RECORDED, ctx);
    }

    private static String truncate(String s) {
        return s != null && s.length() > 500 ? s.substring(0, 500) : s;
    }
}
