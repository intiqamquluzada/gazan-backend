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
        name = "promotions",
        indexes = {
                @Index(name = "idx_promotions_active", columnList = "active"),
                @Index(name = "idx_promotions_ends", columnList = "ends_at"),
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "tag", nullable = false, length = 40)
    private String tag;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "subtitle", length = 200)
    private String subtitle;

    @Column(name = "emoji", length = 8)
    private String emoji;

    @Column(name = "gradient_start_hex", nullable = false)
    private long gradientStartHex;

    @Column(name = "gradient_end_hex", nullable = false)
    private long gradientEndHex;

    @Column(name = "cta", length = 40)
    private String cta;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
