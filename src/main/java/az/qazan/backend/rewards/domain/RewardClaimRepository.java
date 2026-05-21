package az.qazan.backend.rewards.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RewardClaimRepository extends JpaRepository<RewardClaim, UUID> {

    List<RewardClaim> findAllByUserIdAndStatusOrderByCreatedAtDesc(
            UUID userId, RewardClaimStatus status);

    List<RewardClaim> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    List<RewardClaim> findAllByUserIdAndCompanyIdAndStatusOrderByCreatedAtDesc(
            UUID userId, UUID companyId, RewardClaimStatus status);
}
