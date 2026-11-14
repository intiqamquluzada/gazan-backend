package az.qazan.backend.rewards.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpendSessionRepository extends JpaRepository<SpendSession, UUID> {

    Optional<SpendSession> findFirstByCompanyIdAndUserIdAndStatus(
            UUID companyId, UUID userId, SpendSessionStatus status);

    List<SpendSession> findAllByCompanyIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
            UUID companyId, SpendSessionStatus status, Instant now);
}
