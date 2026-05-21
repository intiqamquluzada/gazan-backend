package az.qazan.backend.rewards.application;

import az.qazan.backend.coins.domain.CoinReward;
import az.qazan.backend.coins.domain.CoinRewardRepository;
import az.qazan.backend.coins.domain.CoinTransaction;
import az.qazan.backend.coins.domain.CoinTransactionRepository;
import az.qazan.backend.coins.domain.CoinTransactionType;
import az.qazan.backend.common.exception.BadRequestException;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.NotFoundException;
import az.qazan.backend.common.exception.UnauthorizedException;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.companies.domain.CompanyRepository;
import az.qazan.backend.loyalty.application.LoyaltyCardService;
import az.qazan.backend.loyalty.domain.LoyaltyCard;
import az.qazan.backend.loyalty.domain.LoyaltyCardRepository;
import az.qazan.backend.notifications.application.NotificationService;
import az.qazan.backend.rewards.api.dto.RewardClaimResponse;
import az.qazan.backend.rewards.domain.RewardClaim;
import az.qazan.backend.rewards.domain.RewardClaimRepository;
import az.qazan.backend.rewards.domain.RewardClaimStatus;
import az.qazan.backend.rewards.domain.RewardSource;
import az.qazan.backend.user.domain.User;
import az.qazan.backend.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
            throw new BadRequestException(ErrorCode.BAD_REQUEST);
        }
        Company company = reward.getCompany();
        long balance = ledger.balanceOfAtCompany(userId, company.getId());
        if (balance < reward.getCoinCost()) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST);
        }
        User user = users.findById(userId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.USER_NOT_FOUND));

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

        notifications.notifyUser(userId, "Hədiyyə alındı",
                reward.getTitle() + " — kassada QR kodunu göstər və təsdiqlət.");

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

    /**
     * Vouchers the customer can actually use AT this business right now.
     * Used by the QR-scan sheet on the business side.
     */
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

    // ─────────────────────── Cashier side ───────────────────────

    /**
     * Cashier marks a customer's voucher as used. Validates that the
     * cashier owns the company the voucher belongs to. Sends a per-user
     * notification.
     */
    @Transactional
    public RewardClaimResponse use(UUID ownerId, String kind,
                                   UUID id, UUID customerId) {
        if ("COIN".equals(kind)) {
            RewardClaim claim = claims.findById(id)
                    .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
            if (!claim.getUserId().equals(customerId)) {
                throw new BadRequestException(ErrorCode.BAD_REQUEST);
            }
            if (claim.getStatus() != RewardClaimStatus.ACTIVE) {
                throw new BadRequestException(ErrorCode.BAD_REQUEST);
            }
            Company c = companies.findById(claim.getCompanyId())
                    .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
            requireOwner(c, ownerId);

            claim.setStatus(RewardClaimStatus.USED);
            claim.setUsedAt(Instant.now());
            RewardClaim saved = claims.save(claim);
            notifications.notifyUser(customerId, "Hədiyyə istifadə olundu",
                    saved.getTitle() + " — " + c.getName() + " kassasında təsdiqləndi.");
            return toCoinResponse(saved, c);
        }
        if ("CARD".equals(kind)) {
            LoyaltyCard card = cards.findById(id)
                    .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
            if (!card.getUser().getId().equals(customerId)) {
                throw new BadRequestException(ErrorCode.BAD_REQUEST);
            }
            if (card.getRewardsAvailable() <= 0) {
                throw new BadRequestException(ErrorCode.BAD_REQUEST);
            }
            Company c = card.getCompany();
            requireOwner(c, ownerId);

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
            notifications.notifyUser(customerId, "Hədiyyə istifadə olundu",
                    title + " — " + c.getName() + " kassasında təsdiqləndi.");
            return toResponseFromClaim(history, c);
        }
        throw new BadRequestException(ErrorCode.BAD_REQUEST);
    }

    // ─────────────────────── helpers ───────────────────────

    private void requireOwner(Company company, UUID ownerId) {
        if (company.getOwner() == null
                || !company.getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException(ErrorCode.FORBIDDEN);
        }
    }

    private String cardRewardTitle(LoyaltyCard card) {
        try {
            String item = card.getProgram().getRewardItem();
            return (item == null || item.isBlank())
                    ? "Sadiqlik mükafatı"
                    : "Pulsuz " + item;
        } catch (Exception ignored) {
            return "Sadiqlik mükafatı";
        }
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
                c.getUsedAt());
    }

    private RewardClaimResponse toResponseFromClaim(RewardClaim c, Company co) {
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
                c.getUsedAt());
    }

    private RewardClaimResponse toCardVirtualResponse(LoyaltyCard card) {
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
                null);
    }
}
