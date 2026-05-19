package az.qazan.backend.admin.domain;

import az.qazan.backend.coins.domain.CoinTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

/**
 * Admin-only platform-wide view over the coin ledger. Separate from
 * {@code CoinTransactionRepository} (which is per-user) so the admin
 * module owns its own aggregate queries.
 */
public interface AdminCoinRepository extends JpaRepository<CoinTransaction, UUID> {

    Page<CoinTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Net coins in circulation across every user (signed sum). */
    @Query("select coalesce(sum(t.amount), 0) from CoinTransaction t")
    long circulating();

    /** Total ever earned (sum of positive entries). */
    @Query("select coalesce(sum(t.amount), 0) from CoinTransaction t where t.amount > 0")
    long totalEarned();

    /** Total ever spent (sum of negative entries, returned as a positive number). */
    @Query("select coalesce(-sum(t.amount), 0) from CoinTransaction t where t.amount < 0")
    long totalSpent();
}
