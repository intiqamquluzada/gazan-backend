package az.qazan.backend.coins.api.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Owner edits one catalog item; null fields stay as they are. */
public record UpdateCoinRewardRequest(
        @Size(min = 1, max = 120) String title,
        @Size(max = 255) String description,
        @Positive Integer coinCost,
        Boolean active
) {
}
