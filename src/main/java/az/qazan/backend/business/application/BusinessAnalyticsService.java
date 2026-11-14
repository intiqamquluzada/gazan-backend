package az.qazan.backend.business.application;

import az.qazan.backend.business.api.dto.BusinessAnalyticsResponse;
import az.qazan.backend.coins.domain.CoinLedgerRow;
import az.qazan.backend.coins.domain.CoinTransactionRepository;
import az.qazan.backend.companies.application.CompanyService;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.loyalty.domain.LoyaltyCardRepository;
import az.qazan.backend.loyalty.domain.LoyaltyEvent;
import az.qazan.backend.loyalty.domain.LoyaltyEventRepository;
import az.qazan.backend.loyalty.domain.LoyaltyEventRow;
import az.qazan.backend.loyalty.domain.LoyaltyProgramRepository;
import az.qazan.backend.loyalty.domain.ProgramCardStats;
import az.qazan.backend.nfc.domain.NfcTap;
import az.qazan.backend.nfc.domain.NfcTapRepository;
import az.qazan.backend.rewards.domain.RewardClaim;
import az.qazan.backend.rewards.domain.RewardClaimRepository;
import az.qazan.backend.rewards.domain.RewardClaimStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessAnalyticsService {

    private static final int MAX_DAYS = 90;

    private final CompanyService companies;
    private final LoyaltyEventRepository events;
    private final LoyaltyCardRepository cards;
    private final LoyaltyProgramRepository programs;
    private final CoinTransactionRepository ledger;
    private final RewardClaimRepository claims;
    private final NfcTapRepository taps;

    @Transactional(readOnly = true)
    public BusinessAnalyticsResponse forOwner(UUID ownerId, int days) {
        Company company = companies.myCompany(ownerId);
        UUID companyId = company.getId();
        ZoneId zone = company.venueZone();
        int window = Math.max(1, Math.min(days, MAX_DAYS));
        LocalDate to = LocalDate.now(zone);
        LocalDate from = to.minusDays(window - 1L);
        Instant since = from.atStartOfDay(zone).toInstant();

        List<LoyaltyEventRow> eventRows = events.rowsAtCompanySince(companyId, since);
        List<CoinLedgerRow> coinRows = ledger.rowsAtCompanySince(companyId, since);
        List<RewardClaim> used = claims.findAllByCompanyIdAndStatusAndUsedAtGreaterThanEqual(
                companyId, RewardClaimStatus.USED, since);

        Map<LocalDate, Long> stampsPerDay = new HashMap<>();
        Map<LocalDate, Long> rewardsPerDay = new HashMap<>();
        Map<LocalDate, Long> earnedPerDay = new HashMap<>();
        Map<UUID, Set<LocalDate>> stampDays = new HashMap<>();
        Set<UUID> active = new HashSet<>();
        long stamps = 0;
        long earned = 0;
        long spent = 0;

        for (LoyaltyEventRow row : eventRows) {
            active.add(row.getUserId());
            if (row.getType() == LoyaltyEvent.Type.STAMP_ADDED) {
                LocalDate day = row.getCreatedAt().atZone(zone).toLocalDate();
                stamps += row.getAmount();
                stampsPerDay.merge(day, (long) row.getAmount(), Long::sum);
                stampDays.computeIfAbsent(row.getUserId(), k -> new HashSet<>()).add(day);
            }
        }
        for (CoinLedgerRow row : coinRows) {
            active.add(row.getUserId());
            if (row.getAmount() > 0) {
                earned += row.getAmount();
                earnedPerDay.merge(row.getCreatedAt().atZone(zone).toLocalDate(),
                        (long) row.getAmount(), Long::sum);
            } else {
                spent -= row.getAmount();
            }
        }
        for (RewardClaim claim : used) {
            rewardsPerDay.merge(claim.getUsedAt().atZone(zone).toLocalDate(), 1L, Long::sum);
        }

        long returning = stampDays.values().stream().filter(d -> d.size() >= 2).count();
        long nfcTotal = taps.countByCompanyIdAndCreatedAtGreaterThanEqual(companyId, since);
        long nfcAccepted = taps.countByCompanyIdAndResultAndCreatedAtGreaterThanEqual(
                companyId, NfcTap.Result.OK, since);

        List<BusinessAnalyticsResponse.Daily> daily = from.datesUntil(to.plusDays(1))
                .map(d -> new BusinessAnalyticsResponse.Daily(
                        d,
                        stampsPerDay.getOrDefault(d, 0L),
                        rewardsPerDay.getOrDefault(d, 0L),
                        earnedPerDay.getOrDefault(d, 0L)))
                .toList();

        Map<UUID, ProgramCardStats> stats = cards.programStats(companyId).stream()
                .collect(Collectors.toMap(ProgramCardStats::getProgramId, Function.identity()));
        List<BusinessAnalyticsResponse.Program> programRows = programs
                .findAllByCompanyId(companyId).stream()
                .map(p -> {
                    ProgramCardStats s = stats.get(p.getId());
                    return new BusinessAnalyticsResponse.Program(
                            p.getId(), p.getTitle(), p.isActive(),
                            s == null ? 0 : s.getMembers(),
                            s == null ? 0 : s.getCompleted(),
                            s == null ? 0 : s.getRedeemed());
                })
                .toList();

        return new BusinessAnalyticsResponse(
                from, to,
                cards.countNewCustomers(companyId, since),
                active.size(), returning, stamps, used.size(), earned, spent,
                nfcAccepted, nfcTotal - nfcAccepted, daily, programRows);
    }
}
