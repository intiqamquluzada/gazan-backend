package az.qazan.backend.promotions.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StoryRepository extends JpaRepository<Story, UUID> {

    List<Story> findAllByActiveTrueOrderByCompanyIdAscSortOrderAsc();

    List<Story> findAllByCompanyIdAndActiveTrueOrderBySortOrderAsc(UUID companyId);
}
