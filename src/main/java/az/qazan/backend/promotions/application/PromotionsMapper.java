package az.qazan.backend.promotions.application;

import az.qazan.backend.promotions.api.dto.PromotionResponse;
import az.qazan.backend.promotions.api.dto.StoryResponse;
import az.qazan.backend.promotions.domain.Promotion;
import az.qazan.backend.promotions.domain.Story;
import org.springframework.stereotype.Component;

@Component
public class PromotionsMapper {

    public StoryResponse toStory(Story s) {
        return new StoryResponse(
                s.getId(),
                s.getCompany().getId(),
                s.getHeadline(),
                s.getBody(),
                s.getEmoji(),
                s.getGradientStartHex(),
                s.getGradientEndHex(),
                s.getCta(),
                s.getDurationSeconds(),
                s.getExpiresAt()
        );
    }

    public PromotionResponse toPromotion(Promotion p) {
        return new PromotionResponse(
                p.getId(),
                p.getCompany() == null ? null : p.getCompany().getId(),
                p.getTag(),
                p.getTitle(),
                p.getSubtitle(),
                p.getEmoji(),
                p.getGradientStartHex(),
                p.getGradientEndHex(),
                p.getCta(),
                p.getEndsAt()
        );
    }
}
