package com.nexora.shared.event;

import java.time.Instant;

/**
 * <h2>Design pattern: Observer (subject side)</h2>
 *
 * Marker for business events published through Spring's {@code ApplicationEventPublisher}.
 * Publishers (inventory, clinic, flight, workflow) do not know who listens; observers
 * (notifications, inventory alerts, dashboard refresh, audit) subscribe with
 * {@code @EventListener} / {@code @TransactionalEventListener}. Events live in {@code shared} so
 * that modules depend on the event contract, not on each other.
 */
public interface DomainEvent {

    Instant occurredAt();

    /** Module that raised the event: INVENTORY, CLINIC, FLIGHT, WORKFLOW. */
    String module();

    /** One line describing what happened, used by the audit trail and the activity feed. */
    String summary();
}
