package az.qazan.backend.notifications.api;

import az.qazan.backend.notifications.api.dto.CreateNotificationRequest;
import az.qazan.backend.notifications.api.dto.NotificationResponse;
import az.qazan.backend.notifications.application.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
public class NotificationAdminController {

    private final NotificationService notifications;

    @Operation(summary = "Broadcast a push notification to every user (admin)")
    @PostMapping
    public NotificationResponse send(
            @Valid @RequestBody CreateNotificationRequest body) {
        return notifications.broadcast(body.title(), body.body());
    }

    @Operation(summary = "List notifications sent (admin)")
    @GetMapping
    public List<NotificationResponse> sent() {
        return notifications.recent();
    }
}
