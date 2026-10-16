package com.nexora.shared.event;

import java.time.Instant;

public record DocumentSubmittedEvent(Long documentId, String number, String title, Long authorId,
                                     String nextApproverRole, String nextStepName,
                                     Instant occurredAt) implements DomainEvent {
    @Override public String module() { return "WORKFLOW"; }
    @Override public String summary() { return "Құжат " + number + " «" + nextStepName + "» қадамына жіберілді"; }
}
