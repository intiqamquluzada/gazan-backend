package az.qazan.backend.coins.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CoinTransactionRepository
        extends JpaRepository<CoinTransaction, UUID> {

    List<CoinTransaction> findTop15ByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("""
        select coalesce(sum(t.amount), 0)
          from CoinTransaction t
         where t.user.id = :uid
        """)
    long balanceOf(@Param("uid") UUID uid);

    @Query("""
        select c.id   as companyId,
               c.name as companyName,
               coalesce(sum(t.amount), 0) as balance
          from CoinTransaction t
          join t.company c
         where t.user.id = :uid
         group by c.id, c.name
         order by sum(t.amount) desc
        """)
    List<CompanyBalanceProjection> balancesByCompany(@Param("uid") UUID uid);
}
