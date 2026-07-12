package az.qazan.backend.notifications.api.dto;

import az.qazan.backend.notifications.domain.NotificationTarget;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Admin / business-owner notification request.
 *
 * <p>{@link #targetType} chooses the audience; the matching target id
 * is required only for that mode:
 * <ul>
 *   <li>{@code BROADCAST} — everyone. {@link #targetUserId} and
 *       {@link #targetCompanyId} ignored.</li>
 *   <li>{@code USER} — {@link #targetUserId} required.</li>
 *   <li>{@code COMPANY_CARDHOLDERS} — {@link #targetCompanyId}
 *       required. Business owners can only target their own
 *       company; admins can target any.</li>
 * </ul>
 *
 * <p>Defaults to {@code BROADCAST} when {@link #targetType} is omitted
 * so existing admin clients keep working.
 */
public record CreateNotificationRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 2000) String body,
        NotificationTarget targetType,
        UUID targetUserId,
        UUID targetCompanyId,
        /// Optional cover image (relative URL from /api/v1/images).
        @Size(max = 512) String imageUrl
) {
    public NotificationTarget effectiveTargetType() {
        return targetType == null ? NotificationTarget.BROADCAST : targetType;
    }
}
