package az.qazan.backend.promotions.api.dto;

import az.qazan.backend.promotions.domain.Promotion;

import java.time.Instant;
import java.util.UUID;

/** Full promotion view for the admin panel (includes active + sortOrder). */
public record AdminPromotionResponse(
        UUID id,
        UUID companyId,
        String tag,
        String title,
        String subtitle,
        String emoji,
        long gradientStartHex,
        long gradientEndHex,
        String cta,
        boolean active,
        Instant endsAt,
        int sortOrder
) {
    public static AdminPromotionResponse from(Promotion p) {
        return new AdminPromotionResponse(
                p.getId(),
                p.getCompany() == null ? null : p.getCompany().getId(),
                p.getTag(),
                p.getTitle(),
                p.getSubtitle(),
                p.getEmoji(),
                p.getGradientStartHex(),
                p.getGradientEndHex(),
                p.getCta(),
                p.isActive(),
                p.getEndsAt(),
                p.getSortOrder()
        );
    }
}
