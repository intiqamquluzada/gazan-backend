package az.qazan.backend.loyalty.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LoyaltyEventRepository extends JpaRepository<LoyaltyEvent, UUID> {

    List<LoyaltyEvent> findAllByCardIdOrderByCreatedAtDesc(UUID cardId);

    @Query("""
           select count(e) from LoyaltyEvent e
            where e.card.company.id = :companyId
              and e.type = :type
              and e.createdAt >= :since
           """)
    long countByCompanySinceWithType(
            @Param("companyId") UUID companyId,
            @Param("type") LoyaltyEvent.Type type,
            @Param("since") Instant since
    );
}
