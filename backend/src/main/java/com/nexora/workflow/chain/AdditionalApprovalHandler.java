package com.nexora.workflow.chain;

import com.nexora.shared.config.PlatformSettings;
import com.nexora.shared.security.Role;
import com.nexora.workflow.entity.DocumentType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 5. High-value documents (and medical reports) need an additional final approval by an administrator. */
@Component
@Order(50)
public class AdditionalApprovalHandler extends DocumentCheckHandler {

    public static final String STEP = "Соңғы бекіту";

    private final PlatformSettings settings;

    public AdditionalApprovalHandler(PlatformSettings settings) {
        this.settings = settings;
    }

    @Override public String name() { return "AdditionalApprovalHandler"; }

    @Override
    protected void check(DocumentReview r) {
        if (r.amount() != null && r.amount().compareTo(settings.additionalApprovalThreshold()) > 0) {
            r.addStep(name(), new DocumentReview.PlannedStep(STEP, Role.ADMIN,
                    "сома " + com.nexora.shared.i18n.Kk.money(settings.additionalApprovalThreshold()) + " шегінен жоғары"));
            return;
        }
        if (r.type() == DocumentType.MEDICAL_APPOINTMENT_REPORT) {
            r.addStep(name(), new DocumentReview.PlannedStep(STEP, Role.ADMIN,
                    "медициналық есепке емхана әкімшісі қосымша қол қояды"));
            return;
        }
        r.record(name(), DocumentReview.Outcome.SKIPPED, "Қосымша бекіту қажет емес");
    }
}
