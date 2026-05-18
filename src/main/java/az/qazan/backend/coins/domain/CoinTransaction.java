package az.qazan.backend.coins.domain;

import az.qazan.backend.common.audit.BaseEntity;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.user.domain.User;
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

/**
 * One entry in a customer's coin ledger.
 *
 * <p>The customer earns coins at every business and spends them on
 * rewards / cash discounts. {@code amount} is signed: positive for
 * {@link CoinTransactionType#EARN}, negative for
 * {@link CoinTransactionType#SPEND}. A balance is therefore just
 * {@code SUM(amount)} — no mutable balance column to keep consistent.
 *
 * <p>{@code company} is nullable: cash-discount / global adjustments are
 * not attributed to any single business.
 */
@Entity
@Table(
        name = "coin_transactions",
        indexes = {
                @Index(name = "idx_coin_user", columnList = "user_id"),
                @Index(name = "idx_coin_user_company", columnList = "user_id, company_id"),
                @Index(name = "idx_coin_created", columnList = "created_at"),
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoinTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    /** Signed: + for EARN, − for SPEND. */
    @Column(name = "amount", nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private CoinTransactionType type;

    @Column(name = "note", length = 240)
    private String note;
}
