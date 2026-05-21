package az.qazan.backend.rewards.domain;

import az.qazan.backend.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A unified "gift voucher" the customer holds: created by a coin
 * purchase (status=ACTIVE) or by the cashier redeeming a stamp-card
 * reward (status=USED, recorded for history). The cashier marks an
 * ACTIVE coin voucher USED when the customer claims it at the counter.
 *
 * <p>userId / companyId / refId are stored as plain UUID columns to
 * keep the rewards module decoupled from the user/company/loyalty
 * modules' entities.
 */
@Entity
@Table(
        name = "reward_claims",
        indexes = {
                @Index(name = "idx_reward_user_status", columnList = "user_id, status"),
                @Index(name = "idx_reward_user_company_status",
                        columnList = "user_id, company_id, status"),
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardClaim extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 16)
    private RewardSource source;

    /** For COIN: the source CoinReward.id. For LOYALTY_CARD: the LoyaltyCard.id. */
    @Column(name = "ref_id")
    private UUID refId;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    /** Coins spent (0 for LOYALTY_CARD-source claims). */
    @Column(name = "coin_cost", nullable = false)
    private int coinCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private RewardClaimStatus status;

    @Column(name = "used_at")
    private Instant usedAt;
}
