package az.qazan.backend.admin.analytics;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Response shapes of the admin analytics endpoints. */
public final class AnalyticsDtos {

    private AnalyticsDtos() {}

    public enum Bucket { DAY, WEEK, MONTH }

    /** A figure for the chosen period beside the same-length period before it. */
    public record Kpi(double value, double previous) {}

    public record Overview(
            LocalDate from,
            LocalDate to,
            LocalDate previousFrom,
            LocalDate previousTo,
            Map<String, Kpi> kpis,
            Totals totals
    ) {}

    /** All-time figures, independent of the chosen period. */
    public record Totals(
            long users,
            long customers,
            long businessOwners,
            long companies,
            long approvedCompanies,
            long pendingCompanies,
            long loyaltyCards,
            long coinsCirculating,
            long nfcTags
    ) {}

    public record Point(
            LocalDate date,
            long newUsers,
            long newCompanies,
            long activeUsers,
            long coinsEarned,
            long coinsSpent,
            long coinTransactions,
            long taps,
            long rejectedTaps,
            long rewardClaims,
            long newCards,
            long reviews
    ) {}

    public record Series(LocalDate from, LocalDate to, Bucket bucket, List<Point> points) {}

    public record Slice(String key, long count) {}

    public record TopCompany(UUID id, String name, String category, long taps,
                             long coinsEarned, long coinsSpent, long rewardClaims,
                             long activeCustomers) {}

    public record TopCustomer(UUID id, String fullName, String email, long taps,
                              long coinsEarned, long coinsSpent, long rewardClaims) {}

    public record Breakdown(
            LocalDate from,
            LocalDate to,
            List<Slice> signupMethods,
            List<Slice> signupRoles,
            List<Slice> companiesByCategory,
            List<Slice> companiesByStatus,
            List<Slice> tapResults,
            List<Slice> ratings,
            List<TopCompany> topCompanies,
            List<TopCustomer> topCustomers,
            /* 7 rows (Mon..Sun) × 24 hours of taps + coin entries, Baku time. */
            long[][] activityHeatmap
    ) {}
}
