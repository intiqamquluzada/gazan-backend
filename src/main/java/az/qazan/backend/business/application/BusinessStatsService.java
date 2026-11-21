package az.qazan.backend.business.application;

import az.qazan.backend.business.api.dto.BusinessStatsResponse;
import az.qazan.backend.coins.domain.CoinLedgerRow;
import az.qazan.backend.coins.domain.CoinTransactionRepository;
import az.qazan.backend.companies.application.CompanyService;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.loyalty.domain.LoyaltyEvent;
import az.qazan.backend.loyalty.domain.LoyaltyEventRepository;
import az.qazan.backend.loyalty.domain.LoyaltyEventRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessStatsService {

    private final CompanyService companies;
    private final LoyaltyEventRepository events;
    private final CoinTransactionRepository ledger;

    @Transactional(readOnly = true)
    public BusinessStatsResponse statsForOwner(UUID ownerId) {
        Company c = companies.myCompany(ownerId);
        UUID companyId = c.getId();
        ZoneId zone = c.venueZone();
        Instant now = Instant.now();
        Instant midnight = LocalDate.now(zone).atStartOfDay(zone).toInstant();

        List<LoyaltyEventRow> rows =
                events.rowsAtCompanySince(companyId, now.minus(Duration.ofDays(30)));
        List<CoinLedgerRow> coins =
                ledger.rowsAtCompanySince(companyId, now.minus(Duration.ofDays(30)));

        Set<UUID> active = new HashSet<>();
        rows.forEach(r -> active.add(r.getUserId()));
        coins.forEach(r -> active.add(r.getUserId()));

        long stampsToday = events.countByCompanySinceWithType(
                companyId, LoyaltyEvent.Type.STAMP_ADDED, midnight);
        long rewardsThisWeek = events.countByCompanySinceWithType(
                companyId, LoyaltyEvent.Type.REWARD_CLAIMED, now.minus(Duration.ofDays(7)));

        // Customers stamped at least twice in 30 days, out of everyone stamped.
        Map<UUID, Long> stampEvents = rows.stream()
                .filter(r -> r.getType() == LoyaltyEvent.Type.STAMP_ADDED)
                .collect(Collectors.groupingBy(
                        LoyaltyEventRow::getUserId, Collectors.counting()));
        long repeaters = stampEvents.values().stream().filter(n -> n >= 2).count();
        double repeatRate = stampEvents.isEmpty()
                ? 0.0 : (double) repeaters / stampEvents.size();

        return new BusinessStatsResponse(
                active.size(), stampsToday, rewardsThisWeek, repeatRate);
    }
}
