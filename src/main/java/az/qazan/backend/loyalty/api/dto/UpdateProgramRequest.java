package az.qazan.backend.loyalty.api.dto;

import az.qazan.backend.loyalty.domain.LoyaltyRewardType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record UpdateProgramRequest(
        @Size(min = 3, max = 120) String title,
        @Size(max = 500) String description,
        @Min(1) @Max(50) Integer stampsRequired,
        LoyaltyRewardType rewardType,
        @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal rewardValue,
        @Size(max = 80) String rewardItem,
        Boolean active,
        Instant expiresAt,
        /** true removes the end date (a null expiresAt alone means "unchanged"). */
        Boolean clearExpiresAt
) {
}
