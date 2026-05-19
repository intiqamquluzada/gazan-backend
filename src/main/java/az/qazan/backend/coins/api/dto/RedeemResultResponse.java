package az.qazan.backend.coins.api.dto;

public record RedeemResultResponse(
        String rewardTitle,
        int coinCost,
        long remainingAtCompany
) {
}
