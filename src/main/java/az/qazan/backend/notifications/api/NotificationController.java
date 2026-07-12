package az.qazan.backend.notifications.api;

import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.common.security.CurrentUser;
import az.qazan.backend.companies.application.CompanyService;
import az.qazan.backend.companies.domain.Company;
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
    private final CompanyService companies;

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

    /**
     * Business owner submits a notification for the customers holding
     * a loyalty card at their company. The request is saved as
     * {@code PENDING} — an admin reviews it from the moderation panel
     * before delivery.
     */
    @Operation(summary =
            "Submit a notification to your company's cardholders (pending admin review)")
    @PreAuthorize("hasRole('BUSINESS_OWNER') or hasRole('ADMIN')")
    @PostMapping("/submit")
    public NotificationResponse submitFromOwner(
            @CurrentUser AppUserPrincipal me,
            @Valid @RequestBody CreateNotificationRequest body) {
        // Owners can only target their own company. We look up the
        // owner's company server-side and ignore any value the client
        // sent for `targetCompanyId`.
        Company my = companies.myCompany(me.getId());
        return notifications.submitByOwner(
                body.title(),
                body.body(),
                body.imageUrl(),
                my.getId(),
                me.getId());
    }
}
