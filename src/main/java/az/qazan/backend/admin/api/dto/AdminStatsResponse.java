package az.qazan.backend.admin.api.dto;

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
        List<AdminCompanyResponse> recentCompanies
) {
}
