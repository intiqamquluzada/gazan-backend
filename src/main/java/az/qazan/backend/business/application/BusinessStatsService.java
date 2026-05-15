package az.qazan.backend.business.application;

import az.qazan.backend.business.api.dto.BusinessStatsResponse;
import az.qazan.backend.companies.application.CompanyService;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.loyalty.domain.LoyaltyCardRepository;
import az.qazan.backend.loyalty.domain.LoyaltyEvent;
import az.qazan.backend.loyalty.domain.LoyaltyEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BusinessStatsService {

    private final CompanyService companies;
    private final LoyaltyCardRepository cards;
    private final LoyaltyEventRepository events;

    @Transactional(readOnly = true)
    public BusinessStatsResponse statsForOwner(UUID ownerId) {
        Company c = companies.myCompany(ownerId);
        UUID companyId = c.getId();
        Instant startOfToday = Instant.now().minus(Duration.ofHours(24));
        Instant startOfWeek = Instant.now().minus(Duration.ofDays(7));

        int activeCustomers = cards.countByCompanyId(companyId);
        long stampsToday = events.countByCompanySinceWithType(
                companyId, LoyaltyEvent.Type.STAMP_ADDED, startOfToday);
        long rewardsThisWeek = events.countByCompanySinceWithType(
                companyId, LoyaltyEvent.Type.REWARD_CLAIMED, startOfWeek);

        // Repeat-rate placeholder until we add proper visit aggregation.
        double repeatRate = activeCustomers == 0 ? 0.0
                : Math.min(1.0, (double) stampsToday / Math.max(1, activeCustomers));

        return new BusinessStatsResponse(
                activeCustomers, stampsToday, rewardsThisWeek, repeatRate
        );
    }
}
