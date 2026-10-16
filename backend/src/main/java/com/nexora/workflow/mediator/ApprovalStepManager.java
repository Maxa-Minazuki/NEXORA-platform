package com.nexora.workflow.mediator;

import com.nexora.shared.exception.ConflictException;
import com.nexora.shared.exception.ForbiddenOperationException;
import com.nexora.shared.security.Role;
import com.nexora.workflow.chain.DocumentReview;
import com.nexora.workflow.entity.ApprovalStep;
import com.nexora.workflow.repository.ApprovalStepRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/** Colleague: creates the steps of a round, checks who may decide and records decisions. */
@Component
public class ApprovalStepManager extends WorkflowColleague {

    private final ApprovalStepRepository steps;

    public ApprovalStepManager(ApprovalStepRepository steps) {
        this.steps = steps;
    }

    @Override public String role() { return "ApprovalStepManager"; }

    void openRound(WorkflowContext ctx) {
        List<DocumentReview.PlannedStep> route = ctx.review.steps();
        int round = ctx.document.getApprovalRound();
        for (int i = 0; i < route.size(); i++) {
            var p = route.get(i);
            steps.save(new ApprovalStep(ctx.document.getId(), round, i + 1, p.name(), p.requiredRole(),
                    i == 0 ? ApprovalStep.Status.ACTIVE : ApprovalStep.Status.WAITING));
        }
        ctx.log(role() + ": " + round + "-кезең ашылды, қадамдар: " + route.size());
        report(WorkflowSignal.STEPS_UPDATED, ctx);
    }

    /** Approver must hold the step's role (ADMIN may act on any step) and must not be the author. */
    public static boolean canDecide(ApprovalStep step, Long userId, Role role, Long authorId) {
        if (step == null || step.getStatus() != ApprovalStep.Status.ACTIVE || userId.equals(authorId)) {
            return false;
        }
        return role == Role.ADMIN || role == step.getRequiredRole();
    }

    void decide(WorkflowContext ctx) {
        List<ApprovalStep> round = steps.findByDocumentIdAndApprovalRoundOrderByStepOrderAsc(ctx.document.getId(),
                ctx.document.getApprovalRound());
        ApprovalStep active = round.stream().filter(s -> s.getStatus() == ApprovalStep.Status.ACTIVE).findFirst()
                .orElseThrow(() -> new ConflictException("NO_ACTIVE_STEP", "Бұл құжатта шешім күтіп тұрған қадам жоқ"));
        if (ctx.actorId.equals(ctx.document.getAuthorId())) {
            throw new ForbiddenOperationException("Автор өз құжаты бойынша шешім қабылдай алмайды");
        }
        if (!canDecide(active, ctx.actorId, ctx.actorRole, ctx.document.getAuthorId())) {
            throw new ForbiddenOperationException("«" + active.getName() + "» қадамы бойынша шешімді "
                    + com.nexora.shared.i18n.Kk.role(active.getRequiredRole().name()) + " қабылдауы керек");
        }
        ApprovalStep.Status outcome = switch (ctx.action) {
            case APPROVE -> ApprovalStep.Status.APPROVED;
            case REJECT -> ApprovalStep.Status.REJECTED;
            case RETURN -> ApprovalStep.Status.RETURNED;
            case SUBMIT -> throw new IllegalStateException("SUBMIT is not a decision");
        };
        active.decide(outcome, ctx.actorId, ctx.comment, ctx.now);
        ctx.decidedStep = active;
        if (outcome == ApprovalStep.Status.APPROVED) {
            ctx.nextStep = round.stream().filter(s -> s.getStatus() == ApprovalStep.Status.WAITING).findFirst().orElse(null);
            ctx.lastStep = ctx.nextStep == null;
            if (ctx.nextStep != null) {
                ctx.nextStep.activate();
            }
        } else {
            round.stream().filter(s -> s.getStatus() == ApprovalStep.Status.WAITING).forEach(ApprovalStep::skip);
        }
        ctx.log(role() + ": «" + active.getName() + "» қадамы - " + outcome
                + (ctx.nextStep != null ? ", келесісі: " + ctx.nextStep.getName() : ""));
        report(WorkflowSignal.STEPS_UPDATED, ctx);
    }
}
