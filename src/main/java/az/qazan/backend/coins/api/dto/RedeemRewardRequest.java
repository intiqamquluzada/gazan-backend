package az.qazan.backend.coins.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Cashier confirms a customer claiming a coin reward (after a scan). */
public record RedeemRewardRequest(
        @NotNull UUID customerId,
        @NotNull UUID rewardId
) {
}
