package az.qazan.backend.coins.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CoinRewardRepository extends JpaRepository<CoinReward, UUID> {

    List<CoinReward> findAllByCompanyIdOrderByCoinCostAsc(UUID companyId);

    List<CoinReward> findAllByCompanyIdAndActiveTrueOrderByCoinCostAsc(
            UUID companyId);

    /** Every active reward across all businesses (company eagerly fetched). */
    @Query("""
        select r from CoinReward r
          join fetch r.company c
         where r.active = true
         order by c.name asc, r.coinCost asc
        """)
    List<CoinReward> findAllActiveWithCompany();
}
