package az.qazan.backend.admin.api.dto;

import az.qazan.backend.admin.domain.AdminAuditEntry;

import java.time.Instant;
import java.util.UUID;

public record AdminAuditResponse(
        UUID id,
        UUID actorId,
        String actorEmail,
        String action,
        String targetType,
        String targetId,
        String detail,
        Instant createdAt
) {
    public static AdminAuditResponse from(AdminAuditEntry e) {
        return new AdminAuditResponse(
                e.getId(), e.getActorId(), e.getActorEmail(), e.getAction(),
                e.getTargetType(), e.getTargetId(), e.getDetail(), e.getCreatedAt());
    }
}
