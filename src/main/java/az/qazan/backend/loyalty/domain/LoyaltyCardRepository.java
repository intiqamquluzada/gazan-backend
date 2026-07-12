package az.qazan.backend.loyalty.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoyaltyCardRepository extends JpaRepository<LoyaltyCard, UUID> {

    List<LoyaltyCard> findAllByUserIdOrderByLastActivityAtDesc(UUID userId);

    Optional<LoyaltyCard> findByUserIdAndProgramId(UUID userId, UUID programId);

    int countByCompanyId(UUID companyId);

    /**
     * Every card a customer holds at one company. Used by the business
     * dashboard's customer-detail view (one row per loyalty program).
     */
    List<LoyaltyCard> findAllByUserIdAndCompanyIdOrderByLastActivityAtDesc(
            UUID userId, UUID companyId);
}
