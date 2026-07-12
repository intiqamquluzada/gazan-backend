package az.qazan.backend.notifications.api;

import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
public class NotificationAdminController {

    private final NotificationService notifications;

    /**
     * Admin creates a notification. The body's {@code targetType}
     * chooses BROADCAST / USER / COMPANY_CARDHOLDERS — admins can
     * target any user or company. Stored as APPROVED so it's
     * delivered immediately.
     */
    @Operation(summary = "Create + send a notification (admin)")
    @PostMapping
    public NotificationResponse send(
            @CurrentUser AppUserPrincipal me,
            @Valid @RequestBody CreateNotificationRequest body) {
        return notifications.createByAdmin(
                body.title(),
                body.body(),
                body.effectiveTargetType(),
                body.targetUserId(),
                body.targetCompanyId(),
                body.imageUrl(),
                me.getId());
    }

    @Operation(summary = "List notifications already delivered (admin)")
    @GetMapping
    public List<NotificationResponse> sent() {
        return notifications.recent();
    }

    @Operation(summary = "List business-owner notifications waiting for approval")
    @GetMapping("/pending")
    public List<NotificationResponse> pending() {
        return notifications.pending();
    }

    @Operation(summary = "Approve a pending notification — it goes out immediately")
    @PostMapping("/{id}/approve")
    public NotificationResponse approve(
            @CurrentUser AppUserPrincipal me,
            @PathVariable UUID id) {
        return notifications.approve(id, me.getId());
    }

    @Operation(summary = "Reject a pending notification (kept for audit)")
    @PostMapping("/{id}/reject")
    public NotificationResponse reject(
            @CurrentUser AppUserPrincipal me,
            @PathVariable UUID id) {
        return notifications.reject(id, me.getId());
    }
}
