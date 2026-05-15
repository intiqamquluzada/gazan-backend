package az.qazan.backend.business.api.dto;

public record BusinessStatsResponse(
        int activeCustomers,
        long stampsToday,
        long rewardsThisWeek,
        double repeatRate
) {
}
