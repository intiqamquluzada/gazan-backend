package az.qazan.backend.notifications.application;

import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.AppException;
import az.qazan.backend.common.exception.NotFoundException;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.companies.domain.CompanyRepository;
import az.qazan.backend.notifications.api.dto.NotificationResponse;
import az.qazan.backend.notifications.domain.Notification;
import az.qazan.backend.notifications.domain.NotificationRead;
import az.qazan.backend.notifications.domain.NotificationReadRepository;
import az.qazan.backend.notifications.domain.NotificationRepository;
import az.qazan.backend.notifications.domain.NotificationStatus;
import az.qazan.backend.notifications.domain.NotificationTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notifications;
    private final NotificationReadRepository reads;
    private final CompanyRepository companies;

    // ── Admin authoring ─────────────────────────────────────────────

    /**
     * Admin-authored notification. Saved with {@code APPROVED} so it's
     * delivered immediately. {@code request} carries the target type
     * and (when applicable) the target id.
     */
    @Transactional
    public NotificationResponse createByAdmin(
            String title,
            String body,
            NotificationTarget target,
            UUID targetUserId,
            UUID targetCompanyId,
            String imageUrl,
            UUID adminId) {
        Notification n = buildAndValidate(title, body, target,
                targetUserId, targetCompanyId, /*ownerScope*/ null);
        n.setImageUrl(blankToNull(imageUrl));
        n.setStatus(NotificationStatus.APPROVED);
        n.setApprovedBy(adminId);
        n.setApprovedAt(OffsetDateTime.now());
        n.setSubmittedBy(adminId);
        return NotificationResponse.of(notifications.save(n), false);
    }

    /**
     * Convenience for system-emitted user notifications (e.g. "your
     * reward was used"). Kept on the existing signature so the
     * callers in other modules don't need updating.
     */
    @Transactional
    public Notification notifyUser(UUID userId, String title, String body) {
        Notification n = Notification.builder()
                .userId(userId)
                .title(title.trim())
                .body(body.trim())
                .targetType(NotificationTarget.USER)
                .status(NotificationStatus.APPROVED)
                .approvedAt(OffsetDateTime.now())
                .build();
        return notifications.save(n);
    }

    // ── Business-owner submissions ──────────────────────────────────

    /**
     * Business owner asks to push a notification to the customers
     * holding a loyalty card at their company. Saved as {@code PENDING}
     * — an admin reviews it before delivery.
     */
    @Transactional
    public NotificationResponse submitByOwner(
            String title,
            String body,
            String imageUrl,
            UUID companyId,
            UUID ownerId) {
        Notification n = buildAndValidate(title, body,
                NotificationTarget.COMPANY_CARDHOLDERS,
                /*targetUser*/ null, companyId, ownerId);
        n.setImageUrl(blankToNull(imageUrl));
        n.setStatus(NotificationStatus.PENDING);
        n.setSubmittedBy(ownerId);
        return NotificationResponse.of(notifications.save(n), false);
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // ── Admin moderation ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<NotificationResponse> pending() {
        return notifications.findPending().stream()
                .map(n -> NotificationResponse.of(n, false))
                .toList();
    }

    @Transactional
    public NotificationResponse approve(UUID notificationId, UUID adminId) {
        Notification n = notifications.findById(notificationId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND));
        if (n.getStatus() != NotificationStatus.PENDING) {
            return NotificationResponse.of(n, false);
        }
        n.setStatus(NotificationStatus.APPROVED);
        n.setApprovedBy(adminId);
        n.setApprovedAt(OffsetDateTime.now());
        return NotificationResponse.of(n, false);
    }

    @Transactional
    public NotificationResponse reject(UUID notificationId, UUID adminId) {
        Notification n = notifications.findById(notificationId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND));
        if (n.getStatus() != NotificationStatus.PENDING) {
            return NotificationResponse.of(n, false);
        }
        n.setStatus(NotificationStatus.REJECTED);
        n.setApprovedBy(adminId);
        n.setApprovedAt(OffsetDateTime.now());
        return NotificationResponse.of(n, false);
    }

    // ── Read paths ──────────────────────────────────────────────────

    /** Admin's "sent" list — 50 newest approved rows. */
    @Transactional(readOnly = true)
    public List<NotificationResponse> recent() {
        return notifications.findApprovedRecent(PageRequest.of(0, 50)).stream()
                .map(n -> NotificationResponse.of(n, false))
                .toList();
    }

    /** A user's inbox — broadcasts, direct, and cardholder targets. */
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

    // ── helpers ─────────────────────────────────────────────────────

    /**
     * Validates the target + (when ownerScope is set) checks the owner
     * actually owns the company they're targeting. Returns a
     * partially-built Notification — caller still sets status.
     */
    private Notification buildAndValidate(
            String title,
            String body,
            NotificationTarget target,
            UUID targetUserId,
            UUID targetCompanyId,
            UUID ownerScope) {
        Notification.NotificationBuilder b = Notification.builder()
                .title(title.trim())
                .body(body.trim())
                .targetType(target);

        switch (target) {
            case BROADCAST -> {
                // no extra ids
            }
            case USER -> {
                if (targetUserId == null) {
                    throw new AppException(ErrorCode.BAD_REQUEST);
                }
                b.userId(targetUserId);
            }
            case COMPANY_CARDHOLDERS -> {
                if (targetCompanyId == null) {
                    throw new AppException(ErrorCode.BAD_REQUEST);
                }
                Company c = companies.findById(targetCompanyId)
                        .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND));
                if (ownerScope != null &&
                        (c.getOwner() == null
                                || !c.getOwner().getId().equals(ownerScope))) {
                    throw new AppException(ErrorCode.FORBIDDEN);
                }
                b.targetCompanyId(targetCompanyId);
            }
        }
        return b.build();
    }
}
