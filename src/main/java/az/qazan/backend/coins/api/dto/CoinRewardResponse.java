package az.qazan.backend.coins.api.dto;

import java.util.UUID;

public record CoinRewardResponse(
        UUID id,
        UUID companyId,
        String title,
        String description,
        int coinCost,
        boolean active
) {
}
