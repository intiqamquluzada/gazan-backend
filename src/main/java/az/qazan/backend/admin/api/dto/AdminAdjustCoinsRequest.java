package az.qazan.backend.admin.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Admin manually corrects a user's coin balance. {@code amount} is signed:
 * positive credits, negative debits. {@code companyId} optionally attributes
 * the adjustment to a business (null = platform-level correction).
 */
public record AdminAdjustCoinsRequest(
        @NotNull UUID userId,
        UUID companyId,
        @NotNull @Min(-1000000) @Max(1000000) Integer amount,
        @Size(max = 200) String note
) {
}
