package com.nexora.audit.service;

import com.nexora.audit.entity.AuditLog;
import com.nexora.audit.repository.AuditLogRepository;
import com.nexora.shared.command.AuditTrail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** Persists audit entries in their own transaction so failed (rolled back) actions are still logged. */
@Service
public class AuditService implements AuditTrail {

    private final AuditLogRepository repository;
    private final Clock clock;

    public AuditService(AuditLogRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long actorId, String actorUsername, String action, String entityType, String entityId,
                       Outcome outcome, String details) {
        String safeDetails = details == null ? null : (details.length() > 1000 ? details.substring(0, 1000) : details);
        repository.save(new AuditLog(actorId, actorUsername, action, entityType, entityId, outcome.name(),
                safeDetails, clock.instant()));
    }
}
