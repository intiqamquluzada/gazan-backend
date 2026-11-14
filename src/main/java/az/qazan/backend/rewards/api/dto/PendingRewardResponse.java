package az.qazan.backend.rewards.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * One reward waiting to be handed over at the counter, with the customer's
 * name so the cashier can find the person without scanning anything.
 *
 * <p>This replaces the old "scan the customer's QR" step: with NFC the
 * venue's card does the earning, and redemption is picked from this list.
 */
public record PendingRewardResponse(
        String kind,          // "COIN" | "CARD"
        UUID id,              // COIN: claim id · CARD: loyalty card id
        UUID customerId,
        String customerName,
        String customerPhone,
        String title,
        int coinCost,
        Instant createdAt,
        // How the reward is described: FREE_ITEM | PERCENTAGE_DISCOUNT |
        // FIXED_DISCOUNT | CASHBACK for stamp gifts, COIN for bought vouchers.
        String rewardType,
        java.math.BigDecimal rewardValue,
        String rewardItem,
        String currency
) {
}
