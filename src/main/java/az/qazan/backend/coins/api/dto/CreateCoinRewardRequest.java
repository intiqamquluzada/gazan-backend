package az.qazan.backend.coins.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateCoinRewardRequest(
        @NotBlank @Size(max = 120) String title,
        @Size(max = 255) String description,
        @Positive int coinCost
) {
}
