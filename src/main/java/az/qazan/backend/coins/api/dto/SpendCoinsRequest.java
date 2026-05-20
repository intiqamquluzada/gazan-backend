package az.qazan.backend.coins.api.dto;

import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * Spend coins on a reward / cash discount. {@code companyId} is optional —
 * omit it for a global cash discount that isn't tied to one business.
 */
public record SpendCoinsRequest(
        @Positive int amount,
        UUID companyId,
        String note
) {
}
