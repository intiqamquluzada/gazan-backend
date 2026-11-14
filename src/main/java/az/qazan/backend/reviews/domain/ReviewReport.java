package az.qazan.backend.reviews.domain;

import az.qazan.backend.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** A user's report on a review, reviewed by an admin. */
@Entity
@Table(
        name = "review_reports",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_review_report",
                columnNames = {"review_id", "user_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewReport extends BaseEntity {

    @Column(name = "review_id", nullable = false)
    private UUID reviewId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 16)
    private ReviewReportReason reason;

    @Column(name = "note", length = 500)
    private String note;
}
