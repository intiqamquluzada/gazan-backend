package az.qazan.backend.rewards.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/**
 * Cashier marks a customer's voucher as used. {@code kind} routes the
 * action: COIN updates the claim row; CARD redeems the loyalty card and
 * records a USED claim for history.
 */
public record UseRewardRequest(
        @NotBlank @Pattern(regexp = "COIN|CARD") String kind,
        @NotNull UUID id,
        @NotNull UUID customerId
) {
}
