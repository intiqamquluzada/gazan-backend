package az.qazan.backend.coins.api.dto;

import az.qazan.backend.coins.domain.CoinReward;

import java.util.UUID;

/**
 * One reward in the platform-wide catalog shown in the customer wallet
 * ("Hədiyyələrim"). Carries the owning business so the customer can jump
 * to it and redeem at the cashier.
 */
public record CoinRewardCatalogResponse(
        UUID id,
        UUID companyId,
        String companyName,
        String companyLogoUrl,
        String title,
        String description,
        int coinCost
) {
    /** Must be called with the company association initialized. */
    public static CoinRewardCatalogResponse from(CoinReward r) {
        return new CoinRewardCatalogResponse(
                r.getId(),
                r.getCompany().getId(),
                r.getCompany().getName(),
                r.getCompany().getLogoUrl(),
                r.getTitle(),
                r.getDescription(),
                r.getCoinCost());
    }
}
