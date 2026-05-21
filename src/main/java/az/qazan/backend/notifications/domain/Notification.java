package az.qazan.backend.notifications.domain;

import az.qazan.backend.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A notification: either a global broadcast (when {@code userId} is
 * null) or targeted to one user (e.g. "your reward was used"). Read
 * state is tracked per user in {@link NotificationRead}.
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

    /** Null = broadcast (everyone sees it). Set = targeted to this user. */
    @Column(name = "user_id")
    private java.util.UUID userId;
}
