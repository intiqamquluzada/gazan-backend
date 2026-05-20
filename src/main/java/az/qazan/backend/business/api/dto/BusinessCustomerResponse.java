package az.qazan.backend.business.api.dto;

import java.time.Instant;
import java.util.UUID;

public record BusinessCustomerResponse(
        UUID userId,
        String fullName,
        String phone,
        long totalStamps,
        long rewardsClaimed,
        long cardCount,
        Instant lastActivityAt
) {
}
