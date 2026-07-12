package az.qazan.backend.notifications.domain;

import az.qazan.backend.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One notification record. Combined with {@link #targetType} the row
 * describes who should receive it:
 * <ul>
 *   <li>{@link NotificationTarget#BROADCAST} — every user.</li>
 *   <li>{@link NotificationTarget#USER} — only {@link #userId}.</li>
 *   <li>{@link NotificationTarget#COMPANY_CARDHOLDERS} — every user
 *       who currently holds a loyalty card at
 *       {@link #targetCompanyId}.</li>
 * </ul>
 *
 * <p>{@link #status} controls whether it's actually delivered. Admins
 * post {@code APPROVED}; business owners post {@code PENDING} and an
 * admin flips it to {@code APPROVED} (or {@code REJECTED}) from the
 * requested-notifications panel.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "body", nullable = false, length = 2000)
    private String body;

    /** Optional cover image (relative URL from /api/v1/images). */
    @Column(name = "image_url", length = 512)
    private String imageUrl;

    /** Set when {@link #targetType} is {@code USER}. */
    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 32)
    private NotificationTarget targetType;

    /** Set when {@link #targetType} is {@code COMPANY_CARDHOLDERS}. */
    @Column(name = "target_company_id")
    private UUID targetCompanyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    /** Author when a business owner created the request. Null for admin. */
    @Column(name = "submitted_by")
    private UUID submittedBy;

    /** Admin who flipped {@link #status} to APPROVED or REJECTED. */
    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;
}
