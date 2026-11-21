package az.qazan.backend.rewards.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** One customer's window on the cashier screen. */
public record SpendSessionResponse(
        UUID id,
        UUID customerId,
        String customerName,
        String customerPhone,
        String avatarUrl,
        Instant createdAt,
        Instant expiresAt,
        long coinBalance,
        List<Card> cards,
        List<Reward> rewards
) {
    public record Card(String programTitle, int stamps, int stampsRequired, int rewardsAvailable) {}

    public record Reward(String kind, UUID id, String title, int coinCost,
                         String rewardType, java.math.BigDecimal rewardValue,
                         String rewardItem, String currency) {}
}
