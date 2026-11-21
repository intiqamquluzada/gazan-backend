package az.qazan.backend.coins.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Staff credit coins to a customer by hand at their own company. */
public record GrantCoinsRequest(
        @NotNull UUID customerId,
        @NotNull UUID companyId,
        @Positive int amount,
        @Size(max = 240) String note
) {
}
