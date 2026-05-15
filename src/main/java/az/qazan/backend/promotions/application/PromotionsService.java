package az.qazan.backend.promotions.application;

import az.qazan.backend.promotions.domain.Promotion;
import az.qazan.backend.promotions.domain.PromotionRepository;
import az.qazan.backend.promotions.domain.Story;
import az.qazan.backend.promotions.domain.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PromotionsService {

    private final StoryRepository stories;
    private final PromotionRepository promotions;

    @Transactional(readOnly = true)
    public Map<UUID, List<Story>> storyGroups() {
        Map<UUID, List<Story>> grouped = new LinkedHashMap<>();
        for (Story s : stories.findAllByActiveTrueOrderByCompanyIdAscSortOrderAsc()) {
            grouped.computeIfAbsent(s.getCompany().getId(), k -> new java.util.ArrayList<>())
                    .add(s);
        }
        return grouped;
    }

    @Transactional(readOnly = true)
    public List<Story> storiesForCompany(UUID companyId) {
        return stories.findAllByCompanyIdAndActiveTrueOrderBySortOrderAsc(companyId);
    }

    @Transactional(readOnly = true)
    public List<Promotion> activePromotions() {
        return promotions.findAllByActiveTrueOrderBySortOrderAsc();
    }
}
