package az.qazan.backend.promotions.api;

import az.qazan.backend.promotions.api.dto.PromotionResponse;
import az.qazan.backend.promotions.api.dto.StoryGroupResponse;
import az.qazan.backend.promotions.api.dto.StoryResponse;
import az.qazan.backend.promotions.application.PromotionsMapper;
import az.qazan.backend.promotions.application.PromotionsService;
import az.qazan.backend.promotions.domain.Story;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Promotions", description = "Stories + ad banners shown on Discover")
public class PromotionsController {

    private final PromotionsService service;
    private final PromotionsMapper mapper;

    @Operation(summary = "Stories grouped by company — feed for the discover strip")
    @GetMapping("/stories")
    public List<StoryGroupResponse> storyGroups() {
        Map<UUID, List<Story>> grouped = service.storyGroups();
        return grouped.entrySet().stream()
                .map(e -> new StoryGroupResponse(
                        e.getKey(),
                        e.getValue().stream().map(mapper::toStory).toList()
                ))
                .toList();
    }

    @Operation(summary = "Stories for a single company — content for the viewer")
    @GetMapping("/companies/{companyId}/stories")
    public List<StoryResponse> companyStories(@PathVariable UUID companyId) {
        return service.storiesForCompany(companyId).stream()
                .map(mapper::toStory).toList();
    }

    @Operation(summary = "Active promotions — banner carousel on Discover")
    @GetMapping("/promotions")
    public List<PromotionResponse> promotions() {
        return service.activePromotions().stream().map(mapper::toPromotion).toList();
    }
}
