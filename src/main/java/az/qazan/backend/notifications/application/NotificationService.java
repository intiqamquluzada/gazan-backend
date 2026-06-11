package az.qazan.backend.notifications.application;

import az.qazan.backend.notifications.api.dto.NotificationResponse;
import az.qazan.backend.notifications.domain.Notification;
import az.qazan.backend.notifications.domain.NotificationRead;
import az.qazan.backend.notifications.domain.NotificationReadRepository;
import az.qazan.backend.notifications.domain.NotificationRepository;
import az.qazan.backend.push.application.PushService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notifications;
    private final NotificationReadRepository reads;
    private final PushService push;

    /** Admin broadcasts to every user (one row, read state is per-user). */
    @Transactional
    public NotificationResponse broadcast(String title, String body) {
        Notification n = notifications.save(Notification.builder()
                .title(title.trim())
                .body(body.trim())
                .build());
        // Also deliver to physical devices (no-op until FCM is configured).
        push.pushToAll(n.getTitle(), n.getBody());
        return NotificationResponse.of(n, false);
    }

    /** Targeted notification to one user (e.g. "your reward was used"). */
    @Transactional
    public Notification notifyUser(UUID userId, String title, String body) {
        Notification n = notifications.save(Notification.builder()
                .userId(userId)
                .title(title.trim())
                .body(body.trim())
                .build());
        push.pushToUser(userId, n.getTitle(), n.getBody());
        return n;
    }

    /** Admin's list of everything sent (newest first). */
    @Transactional(readOnly = true)
    public List<NotificationResponse> recent() {
        return notifications.findTop50ByOrderByCreatedAtDesc().stream()
                .map(n -> NotificationResponse.of(n, false))
                .toList();
    }

    /** A user's inbox: broadcasts + their targeted notifications. */
    @Transactional(readOnly = true)
    public List<NotificationResponse> inbox(UUID userId) {
        Set<UUID> readIds = reads.readNotificationIds(userId);
        return notifications.inboxFor(userId).stream()
                .map(n -> NotificationResponse.of(n, readIds.contains(n.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notifications.unreadCount(userId);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        for (Notification n : notifications.findUnreadFor(userId)) {
            reads.save(NotificationRead.builder()
                    .notification(n)
                    .userId(userId)
                    .build());
        }
    }
}
