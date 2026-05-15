package az.qazan.backend.promotions.domain;

import az.qazan.backend.common.audit.BaseEntity;
import az.qazan.backend.companies.domain.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "stories",
        indexes = {
                @Index(name = "idx_stories_company", columnList = "company_id"),
                @Index(name = "idx_stories_active", columnList = "active"),
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Story extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "headline", nullable = false, length = 120)
    private String headline;

    @Column(name = "body", length = 240)
    private String body;

    @Column(name = "emoji", length = 8)
    private String emoji;

    @Column(name = "gradient_start_hex", nullable = false)
    private long gradientStartHex;

    @Column(name = "gradient_end_hex", nullable = false)
    private long gradientEndHex;

    @Column(name = "cta", length = 40)
    private String cta;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
