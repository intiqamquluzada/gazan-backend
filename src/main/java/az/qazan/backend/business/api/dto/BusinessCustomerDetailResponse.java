package az.qazan.backend.business.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Rich profile of one customer as the business owner sees them — fed
 * to the customer-detail screen on mobile.
 */
public record BusinessCustomerDetailResponse(
        UUID userId,
        String fullName,
        String email,
        String phone,
        Instant memberSince,
        Instant lastActivityAt,
        int totalStamps,
        int totalRewardsClaimed,
        int rewardsAvailable,
        int totalVisits,
        int cardCount,
        List<CardSummary> cards,
        List<CustomerEvent> recentActivity
) {

    public record CardSummary(
            UUID cardId,
            UUID programId,
            String programTitle,
            int stamps,
            int stampsRequired,
            int rewardsAvailable,
            int totalRewardsClaimed,
            Instant lastActivityAt,
            Instant joinedAt
    ) {}

    public record CustomerEvent(
            UUID id,
            String type,
            int amount,
            String note,
            String programTitle,
            Instant at
    ) {}
}
