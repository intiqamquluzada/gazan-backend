package az.qazan.backend.admin.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Admin manually corrects a user's coin balance. {@code amount} is signed:
 * positive credits, negative debits. {@code companyId} optionally attributes
 * the adjustment to a business (null = platform-level correction).
 */
public record AdminAdjustCoinsRequest(
        @NotNull UUID userId,
        UUID companyId,
        @NotNull Integer amount,
        String note
) {
}
