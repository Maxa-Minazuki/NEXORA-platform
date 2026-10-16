package com.nexora.audit.dto;

import com.nexora.audit.entity.AuditLog;

import java.time.Instant;

public record AuditLogDto(Long id, String actor, String action, String entityType, String entityId, String outcome,
                          String details, Instant createdAt) {

    public static AuditLogDto from(AuditLog a) {
        return new AuditLogDto(a.getId(), a.getActorUsername(), a.getAction(), a.getEntityType(), a.getEntityId(),
                a.getOutcome(), a.getDetails(), a.getCreatedAt());
    }
}
