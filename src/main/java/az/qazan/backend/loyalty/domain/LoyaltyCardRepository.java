package az.qazan.backend.loyalty.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoyaltyCardRepository extends JpaRepository<LoyaltyCard, UUID> {

    List<LoyaltyCard> findAllByUserIdOrderByLastActivityAtDesc(UUID userId);

    Optional<LoyaltyCard> findByUserIdAndProgramId(UUID userId, UUID programId);

    int countByCompanyId(UUID companyId);
}
