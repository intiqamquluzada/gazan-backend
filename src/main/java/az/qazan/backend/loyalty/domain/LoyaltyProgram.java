package az.qazan.backend.loyalty.domain;

import az.qazan.backend.common.audit.BaseEntity;
import az.qazan.backend.companies.domain.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "loyalty_programs",
        indexes = {
                @Index(name = "idx_lp_company", columnList = "company_id"),
                @Index(name = "idx_lp_active", columnList = "active"),
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyProgram extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "stamps_required", nullable = false)
    private int stampsRequired;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false, length = 32)
    private LoyaltyRewardType rewardType;

    @Column(name = "reward_value", precision = 10, scale = 2)
    private BigDecimal rewardValue;

    @Column(name = "reward_item", length = 80)
    private String rewardItem;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "active", nullable = false)
    private boolean active;
}
