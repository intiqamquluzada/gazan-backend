package az.qazan.backend.business.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BusinessAnalyticsResponse(
        LocalDate from,
        LocalDate to,
        long newCustomers,
        long activeCustomers,
        long returningCustomers,
        long stamps,
        long rewardsClaimed,
        long coinsEarned,
        long coinsSpent,
        long nfcAccepted,
        long nfcRejected,
        List<Daily> daily,
        List<Program> programs
) {
    public record Daily(LocalDate date, long stamps, long rewards, long coinsEarned) {}

    public record Program(
            UUID programId,
            String title,
            boolean active,
            long members,
            long completed,
            long redeemed
    ) {}
}
