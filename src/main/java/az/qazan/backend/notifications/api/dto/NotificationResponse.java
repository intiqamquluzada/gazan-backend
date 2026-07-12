package az.qazan.backend.notifications.api.dto;

import az.qazan.backend.notifications.domain.Notification;
import az.qazan.backend.notifications.domain.NotificationStatus;
import az.qazan.backend.notifications.domain.NotificationTarget;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String title,
        String body,
        boolean read,
        Instant createdAt,
        NotificationTarget targetType,
        UUID targetUserId,
        UUID targetCompanyId,
        NotificationStatus status,
        UUID submittedBy,
        String imageUrl
) {
    public static NotificationResponse of(Notification n, boolean read) {
        return new NotificationResponse(
                n.getId(),
                n.getTitle(),
                n.getBody(),
                read,
                n.getCreatedAt(),
                n.getTargetType(),
                n.getUserId(),
                n.getTargetCompanyId(),
                n.getStatus(),
                n.getSubmittedBy(),
                n.getImageUrl()
        );
    }
}
