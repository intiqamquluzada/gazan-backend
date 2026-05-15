package az.qazan.backend.loyalty.api.dto;

import java.time.Instant;
import java.util.UUID;

public record LoyaltyCardResponse(
        UUID id,
        UUID userId,
        UUID companyId,
        UUID programId,
        int stamps,
        int stampsRequired,
        int rewardsAvailable,
        int totalRewardsClaimed,
        Instant lastActivityAt
) {
}
