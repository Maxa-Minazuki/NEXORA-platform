package com.nexora.workflow.chain;

import com.nexora.shared.security.Role;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 6. Final decision: route to the first approver, or approve automatically when nothing requires a
 * human decision. User-filed documents always get at least a manager review (four-eyes principle).
 */
@Component
@Order(100)
public class RoutingDecisionHandler extends DocumentCheckHandler {

    @Override public String name() { return "RoutingDecisionHandler"; }

    @Override
    protected void check(DocumentReview r) {
        if (r.steps().isEmpty() && !r.systemIssued()) {
            r.addStep(name(), new DocumentReview.PlannedStep(ManagerApprovalHandler.STEP, Role.MANAGER, "«төрт көз» қағидаты"));
        }
        if (r.steps().isEmpty()) {
            r.decide(name(), "АВТОБЕКІТУ: жүйелік құжат, бекіту қажет емес");
        } else {
            r.decide(name(), "МАРШРУТ: бекіту қадамдары - " + r.steps().size() + ", біріншісі: " + r.steps().getFirst().name());
        }
    }
}
