package az.qazan.backend.rewards.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * One reward voucher (either a coin-bought ACTIVE voucher, the same
 * voucher after the cashier marked it USED, or a virtual entry generated
 * from a stamp card's {@code rewardsAvailable} count). The mobile UI
 * doesn't care which kind it is — it just renders + tells the backend
 * to "use" it.
 */
public record RewardClaimResponse(
        String kind,        // "COIN" | "CARD"
        UUID id,            // COIN: claim id; CARD: loyalty card id
        UUID companyId,
        String companyName,
        String companyLogoUrl,
        String title,
        int coinCost,       // 0 for CARD
        String status,      // "ACTIVE" | "USED"
        Instant createdAt,
        Instant usedAt      // null when ACTIVE
) {
}
