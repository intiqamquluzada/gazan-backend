package az.qazan.backend.rewards.application;

import az.qazan.backend.coins.domain.CoinReward;
import az.qazan.backend.coins.domain.CoinRewardRepository;
import az.qazan.backend.coins.domain.CoinTransaction;
import az.qazan.backend.coins.domain.CoinTransactionRepository;
import az.qazan.backend.coins.domain.CoinTransactionType;
import az.qazan.backend.common.exception.AppException;
import az.qazan.backend.common.exception.BadRequestException;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.NotFoundException;
import az.qazan.backend.common.security.AppUserPrincipal;
import az.qazan.backend.companies.application.BusinessAccess;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.companies.domain.CompanyRepository;
import az.qazan.backend.loyalty.application.LoyaltyCardService;
import az.qazan.backend.loyalty.domain.LoyaltyCard;
import az.qazan.backend.loyalty.domain.LoyaltyCardRepository;
import az.qazan.backend.notifications.application.NotificationService;
import az.qazan.backend.rewards.api.dto.PendingRewardResponse;
import az.qazan.backend.rewards.api.dto.RewardClaimResponse;
import az.qazan.backend.rewards.domain.RewardClaim;
import az.qazan.backend.rewards.domain.RewardClaimRepository;
import az.qazan.backend.rewards.domain.RewardClaimStatus;
import az.qazan.backend.rewards.domain.RewardSource;
import az.qazan.backend.user.domain.Role;
import az.qazan.backend.user.domain.User;
import az.qazan.backend.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RewardService {

    private final RewardClaimRepository claims;
    private final CoinRewardRepository coinRewards;
    private final CoinTransactionRepository ledger;
    private final CompanyRepository companies;
    private final LoyaltyCardRepository cards;
    private final LoyaltyCardService loyaltyCards;
    private final UserRepository users;
    private final NotificationService notifications;
    private final BusinessAccess access;
    private final RewardTitles titles;

    // ─────────────────────── Customer side ───────────────────────

    /**
     * Customer buys a coin reward. Deducts coins from the per-business
     * balance and creates an ACTIVE voucher. The cashier later marks
     * the voucher USED at the counter.
     */
    @Transactional
    public RewardClaimResponse purchase(UUID userId, UUID coinRewardId) {
        CoinReward reward = coinRewards.findById(coinRewardId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
        if (!reward.isActive()) {
            throw new AppException(ErrorCode.REWARD_INACTIVE);
        }
        Company company = reward.getCompany();
        // Held until commit, so parallel purchases see each other's deduction.
        User user = users.lockById(userId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.USER_NOT_FOUND));
        long balance = ledger.balanceOfAtCompany(userId, company.getId());
        if (balance < reward.getCoinCost()) {
            throw new AppException(ErrorCode.COIN_INSUFFICIENT);
        }

        // Deduct coins via a SPEND ledger entry.
        ledger.save(CoinTransaction.builder()
                .user(user)
                .company(company)
                .amount(-reward.getCoinCost())
                .type(CoinTransactionType.SPEND)
                .note("Hədiyyə: " + reward.getTitle())
                .build());

        RewardClaim claim = claims.save(RewardClaim.builder()
                .userId(userId)
                .companyId(company.getId())
                .source(RewardSource.COIN)
                .refId(reward.getId())
                .title(reward.getTitle())
                .coinCost(reward.getCoinCost())
                .status(RewardClaimStatus.ACTIVE)
                .build());

        notifications.notifyUserLocalized(userId, "notify.gift_bought",
                OPEN_REWARDS, reward.getTitle());

        return toCoinResponse(claim, company);
    }

    /** A user's vouchers: coin claims + virtual card-claims (when active). */
    @Transactional(readOnly = true)
    public List<RewardClaimResponse> mine(UUID userId, RewardClaimStatus filter) {
        List<RewardClaimResponse> out = new ArrayList<>();

        // Coin claims (real RewardClaim rows, both ACTIVE and USED).
        List<RewardClaim> rows = filter == null
                ? claims.findAllByUserIdOrderByCreatedAtDesc(userId)
                : claims.findAllByUserIdAndStatusOrderByCreatedAtDesc(userId, filter);
        for (RewardClaim c : rows) {
            Company co = companies.findById(c.getCompanyId()).orElse(null);
            out.add(toResponseFromClaim(c, co));
        }

        // Virtual ACTIVE card claims — only when caller wants ACTIVE (or all).
        if (filter == null || filter == RewardClaimStatus.ACTIVE) {
            for (LoyaltyCard card :
                    cards.findAllByUserIdOrderByLastActivityAtDesc(userId)) {
                if (card.getRewardsAvailable() > 0) {
                    out.add(toCardVirtualResponse(card));
                }
            }
        }
        return out;
    }

    /** Same list for the business side: the caller must work at this company. */
    @Transactional(readOnly = true)
    public List<RewardClaimResponse> activeAtCompanyForStaff(
            AppUserPrincipal caller, UUID userId, UUID companyId) {
        Company company = companies.findById(companyId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
        if (caller.getRole() != Role.ADMIN) {
            access.requireStaff(company, caller.getId());
        }
        return activeAtCompany(userId, companyId);
    }

    /** Vouchers the customer can actually use AT this business right now. */
    @Transactional(readOnly = true)
    public List<RewardClaimResponse> activeAtCompany(UUID userId, UUID companyId) {
        List<RewardClaimResponse> out = new ArrayList<>();
        for (RewardClaim c : claims
                .findAllByUserIdAndCompanyIdAndStatusOrderByCreatedAtDesc(
                        userId, companyId, RewardClaimStatus.ACTIVE)) {
            Company co = companies.findById(c.getCompanyId()).orElse(null);
            out.add(toResponseFromClaim(c, co));
        }
        for (LoyaltyCard card : cards.findAllByUserIdOrderByLastActivityAtDesc(userId)) {
            if (card.getRewardsAvailable() > 0
                    && card.getCompany().getId().equals(companyId)) {
                out.add(toCardVirtualResponse(card));
            }
        }
        return out;
    }

    /**
      * Everything the cashier can hand out right now at their own venue:
      * bought coin rewards plus completed stamp cards. Ordered newest first.
      */
    @Transactional(readOnly = true)
    public List<PendingRewardResponse> pendingAtMyCompany(UUID ownerId) {
        Company company = access.companyOf(ownerId);
        List<PendingRewardResponse> out = new ArrayList<>();

        for (RewardClaim c : claims.findAllByCompanyIdAndStatusOrderByCreatedAtDesc(
                company.getId(), RewardClaimStatus.ACTIVE)) {
            User u = users.findById(c.getUserId()).orElse(null);
            out.add(new PendingRewardResponse(
                    "COIN",
                    c.getId(),
                    c.getUserId(),
                    u == null ? "—" : u.getFullName(),
                    u == null ? null : u.getPhone(),
                    c.getTitle(),
                    c.getCoinCost(),
                    c.getCreatedAt(),
                    RewardTitles.Info.COIN.rewardType(), null, null, null));
        }
        for (LoyaltyCard card : cards.findPendingAtCompany(company.getId())) {
            User u = card.getUser();
            RewardTitles.Info info = titles.infoOf(card.getProgram());
            for (int i = 0; i < card.getRewardsAvailable(); i++) {
                out.add(new PendingRewardResponse(
                        "CARD",
                        card.getId(),
                        u.getId(),
                        u.getFullName(),
                        u.getPhone(),
                        cardRewardTitle(card),
                        0,
                        card.getLastActivityAt(),
                        info.rewardType(), info.rewardValue(),
                        info.rewardItem(), info.currency()));
            }
        }
        return out;
    }

    // ─────────────────────── Cashier side ───────────────────────

    /**
     * Cashier marks a customer's voucher as used. Validates that the
     * cashier owns the company the voucher belongs to. Sends a per-user
     * notification.
     */
    @Transactional
    public RewardClaimResponse use(UUID ownerId, String kind,
                                   UUID id, UUID customerId) {
        return use(ownerId, kind, id, customerId, null);
    }

    /** {@code expectedCompanyId}, when set, pins the voucher to one venue. */
    @Transactional
    public RewardClaimResponse use(UUID ownerId, String kind, UUID id,
                                   UUID customerId, UUID expectedCompanyId) {
        if ("COIN".equals(kind)) {
            RewardClaim claim = claims.findById(id)
                    .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
            if (!claim.getUserId().equals(customerId)) {
                throw new BadRequestException(ErrorCode.BAD_REQUEST);
            }
            if (claim.getStatus() != RewardClaimStatus.ACTIVE) {
                throw new AppException(ErrorCode.VOUCHER_ALREADY_USED);
            }
            Company c = companies.findById(claim.getCompanyId())
                    .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
            requireVenue(c, expectedCompanyId);
            access.requireStaff(c, ownerId);

            claim.setStatus(RewardClaimStatus.USED);
            claim.setUsedAt(Instant.now());
            RewardClaim saved = claims.save(claim);
            notifications.notifyUserLocalized(customerId, "notify.gift_used",
                    OPEN_REWARDS, saved.getTitle(), c.getName());
            return toCoinResponse(saved, c);
        }
        if ("CARD".equals(kind)) {
            LoyaltyCard card = cards.findById(id)
                    .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
            if (!card.getUser().getId().equals(customerId)) {
                throw new BadRequestException(ErrorCode.BAD_REQUEST);
            }
            if (card.getRewardsAvailable() <= 0) {
                throw new AppException(ErrorCode.VOUCHER_ALREADY_USED);
            }
            Company c = card.getCompany();
            requireVenue(c, expectedCompanyId);
            access.requireStaff(c, ownerId);

            String title = cardRewardTitle(card);
            // Decrement rewardsAvailable + record LoyaltyEvent (İntiqam's logic).
            loyaltyCards.redeem(customerId, card.getId());

            RewardClaim history = claims.save(RewardClaim.builder()
                    .userId(customerId)
                    .companyId(c.getId())
                    .source(RewardSource.LOYALTY_CARD)
                    .refId(card.getId())
                    .title(title)
                    .coinCost(0)
                    .status(RewardClaimStatus.USED)
                    .usedAt(Instant.now())
                    .build());
            Locale customerLocale = notifications.localeOf(customerId);
            notifications.notifyUserLocalized(customerId, "notify.gift_used",
                    OPEN_REWARDS,
                    titles.forProgram(card.getProgram(), customerLocale), c.getName());
            return toResponseFromClaim(history, c);
        }
        throw new BadRequestException(ErrorCode.BAD_REQUEST);
    }

    // ─────────────────────── helpers ───────────────────────

    private static void requireVenue(Company company, UUID expectedCompanyId) {
        if (expectedCompanyId != null && !expectedCompanyId.equals(company.getId())) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST);
        }
    }

    /** Taps on a "gift" notification open the customer's gift list. */
    private static final Map<String, String> OPEN_REWARDS = Map.of("route", "/rewards");

    /** The stamp gift's title in the caller's language, from its reward type. */
    public String cardRewardTitle(LoyaltyCard card) {
        return titles.forProgram(card == null ? null : card.getProgram());
    }

    /** Reward description for one stamp card (type, value, item, currency). */
    public RewardTitles.Info cardRewardInfo(LoyaltyCard card) {
        return titles.infoOf(card == null ? null : card.getProgram());
    }

    private RewardClaimResponse toCoinResponse(RewardClaim c, Company company) {
        return new RewardClaimResponse(
                "COIN",
                c.getId(),
                c.getCompanyId(),
                company == null ? "" : company.getName(),
                company == null ? null : company.getLogoUrl(),
                c.getTitle(),
                c.getCoinCost(),
                c.getStatus().name(),
                c.getCreatedAt(),
                c.getUsedAt(),
                RewardTitles.Info.COIN.rewardType(), null, null, null);
    }

    private RewardClaimResponse toResponseFromClaim(RewardClaim c, Company co) {
        RewardTitles.Info info = c.getSource() == RewardSource.COIN
                ? RewardTitles.Info.COIN
                : (c.getRefId() == null ? java.util.Optional.<LoyaltyCard>empty()
                        : cards.findById(c.getRefId()))
                        .map(this::cardRewardInfo)
                        .orElse(new RewardTitles.Info(null, null, null, null));
        return new RewardClaimResponse(
                c.getSource() == RewardSource.COIN ? "COIN" : "CARD",
                c.getId(),
                c.getCompanyId(),
                co == null ? "" : co.getName(),
                co == null ? null : co.getLogoUrl(),
                c.getTitle(),
                c.getCoinCost(),
                c.getStatus().name(),
                c.getCreatedAt(),
                c.getUsedAt(),
                info.rewardType(), info.rewardValue(), info.rewardItem(), info.currency());
    }

    private RewardClaimResponse toCardVirtualResponse(LoyaltyCard card) {
        RewardTitles.Info info = cardRewardInfo(card);
        Company co = card.getCompany();
        return new RewardClaimResponse(
                "CARD",
                card.getId(),
                co.getId(),
                co.getName(),
                co.getLogoUrl(),
                cardRewardTitle(card),
                0,
                "ACTIVE",
                card.getLastActivityAt(),
                null,
                info.rewardType(), info.rewardValue(), info.rewardItem(), info.currency());
    }
}
