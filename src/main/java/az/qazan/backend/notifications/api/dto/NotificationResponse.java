package az.qazan.backend.notifications.api.dto;

import az.qazan.backend.notifications.domain.Notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String title,
        String body,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse of(Notification n, boolean read) {
        return new NotificationResponse(
                n.getId(), n.getTitle(), n.getBody(), read, n.getCreatedAt());
    }
}
