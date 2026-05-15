package az.qazan.backend.loyalty.api.dto;

import az.qazan.backend.loyalty.domain.LoyaltyRewardType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LoyaltyProgramResponse(
        UUID id,
        UUID companyId,
        String title,
        String description,
        int stampsRequired,
        LoyaltyRewardType rewardType,
        BigDecimal rewardValue,
        String rewardItem,
        Instant expiresAt,
        boolean active
) {
}
