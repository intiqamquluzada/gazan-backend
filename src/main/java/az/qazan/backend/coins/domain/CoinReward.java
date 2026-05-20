package az.qazan.backend.coins.domain;

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

/**
 * A reward a customer can claim with coins at one business
 * (e.g. "100 coin → 1 portion San Sebastian").
 */
@Entity
@Table(
        name = "coin_rewards",
        indexes = {
                @Index(name = "idx_coin_rewards_company", columnList = "company_id"),
                @Index(name = "idx_coin_rewards_active", columnList = "active"),
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoinReward extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "coin_cost", nullable = false)
    private int coinCost;

    @Column(name = "active", nullable = false)
    private boolean active;
}
