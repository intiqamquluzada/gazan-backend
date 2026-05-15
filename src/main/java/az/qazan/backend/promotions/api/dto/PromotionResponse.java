package az.qazan.backend.promotions.api.dto;

import java.time.Instant;
import java.util.UUID;

public record PromotionResponse(
        UUID id,
        UUID companyId,
        String tag,
        String title,
        String subtitle,
        String emoji,
        long gradientStartHex,
        long gradientEndHex,
        String cta,
        Instant endsAt
) {
}
