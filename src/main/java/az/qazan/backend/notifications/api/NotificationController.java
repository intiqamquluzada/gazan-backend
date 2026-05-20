package az.qazan.backend.notifications.api;

import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
import az.qazan.backend.notifications.api.dto.NotificationResponse;
import az.qazan.backend.notifications.application.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * The signed-in user's notification inbox.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notifications;

    @Operation(summary = "My notification inbox (newest 50)")
    @GetMapping
    public List<NotificationResponse> inbox(@CurrentUser AppUserPrincipal me) {
        return notifications.inbox(me.getId());
    }

    @Operation(summary = "My unread notification count")
    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@CurrentUser AppUserPrincipal me) {
        return Map.of("count", notifications.unreadCount(me.getId()));
    }

    @Operation(summary = "Mark all my notifications as read")
    @PostMapping("/read-all")
    public Map<String, Long> markAllRead(@CurrentUser AppUserPrincipal me) {
        notifications.markAllRead(me.getId());
        return Map.of("count", 0L);
    }
}
