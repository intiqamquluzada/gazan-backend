package az.qazan.backend.coins.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CoinRewardRepository extends JpaRepository<CoinReward, UUID> {

    List<CoinReward> findAllByCompanyIdOrderByCoinCostAsc(UUID companyId);

    List<CoinReward> findAllByCompanyIdAndActiveTrueOrderByCoinCostAsc(
            UUID companyId);
}
