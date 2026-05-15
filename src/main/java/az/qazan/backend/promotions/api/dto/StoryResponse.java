package az.qazan.backend.promotions.api.dto;

import java.time.Instant;
import java.util.UUID;

public record StoryResponse(
        UUID id,
        UUID companyId,
        String headline,
        String body,
        String emoji,
        long gradientStartHex,
        long gradientEndHex,
        String cta,
        int durationSeconds,
        Instant expiresAt
) {
}
