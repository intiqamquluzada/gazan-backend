package az.qazan.backend.rewards.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RewardClaimRepository extends JpaRepository<RewardClaim, UUID> {

    List<RewardClaim> findAllByUserIdAndStatusOrderByCreatedAtDesc(
            UUID userId, RewardClaimStatus status);

    List<RewardClaim> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByCompanyIdAndStatus(UUID companyId, RewardClaimStatus status);

    boolean existsByUserIdAndCompanyIdAndStatus(
            UUID userId, UUID companyId, RewardClaimStatus status);

    List<RewardClaim> findAllByCompanyIdAndStatusAndUsedAtGreaterThanEqual(
            UUID companyId, RewardClaimStatus status, java.time.Instant since);

    List<RewardClaim> findAllByUserIdAndCompanyIdAndStatusAndUsedAtGreaterThanEqual(
            UUID userId, UUID companyId, RewardClaimStatus status, java.time.Instant since);

    /** Everything still unused at one venue — the cashier's hand-out list. */
    List<RewardClaim> findAllByCompanyIdAndStatusOrderByCreatedAtDesc(
            UUID companyId, RewardClaimStatus status);

    List<RewardClaim> findAllByUserIdAndCompanyIdAndStatusOrderByCreatedAtDesc(
            UUID userId, UUID companyId, RewardClaimStatus status);
}
