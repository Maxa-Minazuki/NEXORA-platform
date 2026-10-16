package com.nexora.shared.event;

import java.time.Instant;

/** Raised for each approved step; {@code finalApproval} is true when the whole document is approved. */
public record DocumentApprovedEvent(Long documentId, String number, String title, Long authorId, Long approverId,
                                    boolean finalApproval, String nextApproverRole, String nextStepName,
                                    Instant occurredAt) implements DomainEvent {
    @Override public String module() { return "WORKFLOW"; }
    @Override public String summary() {
        return finalApproval ? "Құжат " + number + " бекітілді" : "Құжат " + number + " «" + nextStepName + "» қадамына өтті";
    }
}
