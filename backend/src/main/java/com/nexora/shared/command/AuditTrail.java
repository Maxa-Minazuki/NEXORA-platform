package com.nexora.shared.command;

/**
 * Port through which the command infrastructure records outcomes. Implemented by the audit module,
 * so that {@code shared} does not depend on any business module.
 */
public interface AuditTrail {

    enum Outcome { SUCCESS, FAILURE }

    void record(Long actorId, String actorUsername, String action, String entityType, String entityId,
                Outcome outcome, String details);
}
