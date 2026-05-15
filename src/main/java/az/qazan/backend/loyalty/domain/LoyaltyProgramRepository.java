package az.qazan.backend.loyalty.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoyaltyProgramRepository extends JpaRepository<LoyaltyProgram, UUID> {

    List<LoyaltyProgram> findAllByCompanyIdAndActiveTrue(UUID companyId);

    List<LoyaltyProgram> findAllByCompanyId(UUID companyId);
}
