package az.qazan.backend.loyalty.api.dto;

import az.qazan.backend.loyalty.domain.LoyaltyRewardType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProgramRequest(
        @NotBlank @Size(min = 3, max = 120) String title,
        @Size(max = 500) String description,
        @NotNull @Min(1) @Max(50) Integer stampsRequired,
        @NotNull LoyaltyRewardType rewardType,
        BigDecimal rewardValue,
        @Size(max = 80) String rewardItem
) {
}
