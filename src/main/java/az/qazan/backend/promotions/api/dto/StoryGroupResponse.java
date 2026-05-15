package az.qazan.backend.promotions.api.dto;

import java.util.List;
import java.util.UUID;

public record StoryGroupResponse(
        UUID companyId,
        List<StoryResponse> stories
) {
}
