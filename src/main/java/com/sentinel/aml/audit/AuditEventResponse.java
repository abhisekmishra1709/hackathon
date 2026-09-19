package com.sentinel.aml.audit;

import java.time.Instant;

public record AuditEventResponse(Long id, String entityType, Long entityId, String action, String actorId,
        String details, Instant occurredAt) {
    static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(event.getId(), event.getEntityType(), event.getEntityId(), event.getAction(),
                event.getActorId(), event.getDetails(), event.getOccurredAt());
    }
}