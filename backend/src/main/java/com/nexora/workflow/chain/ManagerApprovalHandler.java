package com.nexora.workflow.chain;

import com.nexora.shared.config.PlatformSettings;
import com.nexora.shared.security.Role;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 4. Decides whether a manager must review (always for some types, otherwise above a threshold). */
@Component
@Order(40)
public class ManagerApprovalHandler extends DocumentCheckHandler {

    public static final String STEP = "Менеджер тексеруі";

    private final PlatformSettings settings;

    public ManagerApprovalHandler(PlatformSettings settings) {
        this.settings = settings;
    }

    @Override public String name() { return "ManagerApprovalHandler"; }

    @Override
    protected void check(DocumentReview r) {
        if (r.type().managerReviewAlways()) {
            r.addStep(name(), new DocumentReview.PlannedStep(STEP, Role.MANAGER,
                    "«" + r.type().label() + "» әрқашан менеджерді қажет етеді"));
            return;
        }
        if (r.amount() != null && r.amount().compareTo(settings.managerApprovalThreshold()) > 0) {
            r.addStep(name(), new DocumentReview.PlannedStep(STEP, Role.MANAGER,
                    "сома " + com.nexora.shared.i18n.Kk.money(settings.managerApprovalThreshold()) + " шегінен жоғары"));
            return;
        }
        r.record(name(), DocumentReview.Outcome.SKIPPED, "Менеджер тексеруі қажет емес");
    }
}
