package az.qazan.backend.coins.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Coins are spent where they were earned, so the company is mandatory. */
public record SpendCoinsRequest(
        @Positive int amount,
        @NotNull UUID companyId,
        @Size(max = 240) String note
) {
}
