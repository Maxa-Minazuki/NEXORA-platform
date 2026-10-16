package com.nexora.shared.event;

import java.time.Instant;

public record DocumentReturnedEvent(Long documentId, String number, String title, Long authorId, Long approverId,
                                    String comment, Instant occurredAt) implements DomainEvent {
    @Override public String module() { return "WORKFLOW"; }
    @Override public String summary() { return "Құжат " + number + " пысықтауға қайтарылды"; }
}
