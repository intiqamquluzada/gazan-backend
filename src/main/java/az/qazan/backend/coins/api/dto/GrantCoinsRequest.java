package az.qazan.backend.coins.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * A business owner credits coins to a customer (after scanning their QR).
 * {@code companyId} is optional but normally the owner's company.
 */
public record GrantCoinsRequest(
        @NotNull UUID customerId,
        UUID companyId,
        @Positive int amount,
        String note
) {
}
