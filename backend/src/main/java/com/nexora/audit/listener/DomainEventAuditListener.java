package com.nexora.audit.listener;

import com.nexora.shared.command.AuditTrail;
import com.nexora.shared.event.DomainEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Observer: every committed business event is written to the audit journal ("EVENT_*" entries). */
@Component
public class DomainEventAuditListener {

    private final AuditTrail auditTrail;

    public DomainEventAuditListener(AuditTrail auditTrail) {
        this.auditTrail = auditTrail;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(DomainEvent event) {
        auditTrail.record(null, "system", "EVENT_" + event.getClass().getSimpleName(), event.module(), null,
                AuditTrail.Outcome.SUCCESS, event.summary());
    }
}
