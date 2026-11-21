package az.qazan.backend.admin.api.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Platform-wide snapshot for the admin dashboard. Read-only aggregate.
 */
public record AdminStatsResponse(
        long totalUsers,
        long customers,
        long businessOwners,
        long admins,
        long totalCompanies,
        long featuredCompanies,
        long totalLoyaltyCards,
        long coinsCirculating,
        long coinsEarned,
        long coinsSpent,
        long coinTransactions,
        List<AdminUserResponse> recentUsers,
        List<AdminCompanyResponse> recentCompanies,
        long newUsers7d,
        long newCompanies7d,
        long pendingCompanies,
        long pendingNotifications,
        long activeUsers30d,
        List<DailyCount> signups
) {
    public record DailyCount(LocalDate date, long count) {}
}
