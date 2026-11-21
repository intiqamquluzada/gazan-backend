package az.qazan.backend.rewards.api.dto;

import java.util.List;
import java.util.UUID;

/** What the customer's own phone shows while the cashier works. */
public record CustomerSpendSessionResponse(
        UUID id,
        String status,
        String companyName,
        List<SpendSessionResponse.Reward> rewards,
        List<String> usedTitles
) {
}
