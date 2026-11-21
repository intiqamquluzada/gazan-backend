package az.qazan.backend.coins.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CoinTransactionRepository
        extends JpaRepository<CoinTransaction, UUID> {

    List<CoinTransaction> findTop15ByUserIdOrderByCreatedAtDesc(UUID userId);

    /** One row per ledger entry at a company, for the business analytics. */
    @Query("""
        select t.amount as amount, t.createdAt as createdAt, t.user.id as userId
          from CoinTransaction t
         where t.company.id = :cid and t.createdAt >= :since
        """)
    List<CoinLedgerRow> rowsAtCompanySince(@Param("cid") UUID cid,
                                           @Param("since") java.time.Instant since);

    @Query("""
        select coalesce(sum(t.amount), 0) from CoinTransaction t
         where t.company.id = :cid and t.amount > 0
        """)
    long totalEarnedAtCompany(@Param("cid") UUID cid);

    @Query("""
        select coalesce(-sum(t.amount), 0) from CoinTransaction t
         where t.company.id = :cid and t.amount < 0
        """)
    long totalSpentAtCompany(@Param("cid") UUID cid);

    @Query("""
        select coalesce(sum(t.amount), 0)
          from CoinTransaction t
         where t.user.id = :uid
        """)
    long balanceOf(@Param("uid") UUID uid);

    @Query("""
        select coalesce(sum(t.amount), 0)
          from CoinTransaction t
         where t.user.id = :uid and t.company.id = :cid
        """)
    long balanceOfAtCompany(@Param("uid") UUID uid, @Param("cid") UUID cid);

    @Query("""
        select c.id      as companyId,
               c.name    as companyName,
               c.logoUrl as logoUrl,
               coalesce(sum(t.amount), 0) as balance
          from CoinTransaction t
          join t.company c
         where t.user.id = :uid
         group by c.id, c.name, c.logoUrl
         order by sum(t.amount) desc
        """)
    List<CompanyBalanceProjection> balancesByCompany(@Param("uid") UUID uid);
}
