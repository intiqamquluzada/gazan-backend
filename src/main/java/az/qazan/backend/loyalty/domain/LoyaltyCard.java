package az.qazan.backend.loyalty.domain;

import az.qazan.backend.common.audit.BaseEntity;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "loyalty_cards",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_card_user_program", columnNames = {"user_id", "program_id"}
        ),
        indexes = {
                @Index(name = "idx_card_user", columnList = "user_id"),
                @Index(name = "idx_card_company", columnList = "company_id"),
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyCard extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private LoyaltyProgram program;

    /** Denormalized for cheap "my cards by company" queries. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "stamps", nullable = false)
    private int stamps;

    /** Snapshot of {@link LoyaltyProgram#getStampsRequired()} taken at join time. */
    @Column(name = "stamps_required", nullable = false)
    private int stampsRequired;

    @Column(name = "rewards_available", nullable = false)
    private int rewardsAvailable;

    @Column(name = "total_rewards_claimed", nullable = false)
    private int totalRewardsClaimed;

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;

    public void addStamp(int amount) {
        int newStamps = this.stamps + amount;
        while (newStamps >= this.stampsRequired) {
            newStamps -= this.stampsRequired;
            this.rewardsAvailable += 1;
        }
        this.stamps = newStamps;
        this.lastActivityAt = Instant.now();
    }

    public void claimOneReward() {
        if (this.rewardsAvailable <= 0) {
            throw new IllegalStateException("No rewards to claim");
        }
        this.rewardsAvailable -= 1;
        this.totalRewardsClaimed += 1;
        this.lastActivityAt = Instant.now();
    }
}
