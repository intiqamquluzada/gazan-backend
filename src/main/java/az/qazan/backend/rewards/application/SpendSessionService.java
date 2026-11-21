package az.qazan.backend.rewards.application;

import az.qazan.backend.coins.domain.CoinTransactionRepository;
import az.qazan.backend.common.exception.AppException;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.NotFoundException;
import az.qazan.backend.companies.application.BusinessAccess;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.companies.domain.CompanyRepository;
import az.qazan.backend.loyalty.domain.LoyaltyCard;
import az.qazan.backend.loyalty.domain.LoyaltyCardRepository;
import az.qazan.backend.common.i18n.MessageService;
import az.qazan.backend.push.application.PushService;
import az.qazan.backend.rewards.api.dto.CustomerSpendSessionResponse;
import az.qazan.backend.rewards.api.dto.SpendSessionResponse;
import az.qazan.backend.rewards.domain.RewardClaim;
import az.qazan.backend.rewards.domain.RewardClaimRepository;
import az.qazan.backend.rewards.domain.RewardClaimStatus;
import az.qazan.backend.rewards.domain.SpendSession;
import az.qazan.backend.rewards.domain.SpendSessionRepository;
import az.qazan.backend.rewards.domain.SpendSessionStatus;
import az.qazan.backend.user.domain.User;
import az.qazan.backend.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SpendSessionService {

    private static final Duration LIFETIME = Duration.ofMinutes(10);

    private final SpendSessionRepository sessions;
    private final RewardClaimRepository claims;
    private final LoyaltyCardRepository cards;
    private final CoinTransactionRepository ledger;
    private final CompanyRepository companies;
    private final UserRepository users;
    private final BusinessAccess access;
    private final RewardService rewards;
    private final PushService push;
    private final MessageService messages;

    // ─────────────────────── opened by the tap ───────────────────────

    @Transactional(readOnly = true)
    public boolean hasUsableRewards(UUID userId, UUID companyId) {
        return claims.existsByUserIdAndCompanyIdAndStatus(
                        userId, companyId, RewardClaimStatus.ACTIVE)
                || cards.findAllByUserIdAndCompanyIdOrderByLastActivityAtDesc(userId, companyId)
                        .stream().anyMatch(c -> c.getRewardsAvailable() > 0);
    }

    /** A second read of the card refreshes the customer's existing window. */
    @Transactional
    public SpendSession openOrRefresh(UUID companyId, UUID userId, UUID tagId) {
        Instant expiresAt = Instant.now().plus(LIFETIME);
        SpendSession open = sessions.findFirstByCompanyIdAndUserIdAndStatus(
                companyId, userId, SpendSessionStatus.OPEN).orElse(null);
        if (open != null && open.isLive()) {
            open.setExpiresAt(expiresAt);
            open.setTagId(tagId);
            return open;
        }
        if (open != null) {
            open.setStatus(SpendSessionStatus.EXPIRED);
            // Flushed first: inserts run before updates, and only one row
            // per customer and venue may be OPEN.
            sessions.saveAndFlush(open);
        }
        SpendSession created = sessions.save(SpendSession.builder()
                .companyId(companyId)
                .userId(userId)
                .tagId(tagId)
                .status(SpendSessionStatus.OPEN)
                .expiresAt(expiresAt)
                .build());
        alertCounter(companyId, userId);
        return created;
    }

    /**
     * Phone push to the owner and cashiers: a customer is waiting at the
     * counter. Sent only when a new window opens (a refresh stays quiet);
     * delivery happens after commit and never fails the tap.
     */
    private void alertCounter(UUID companyId, UUID customerId) {
        List<User> staff = new ArrayList<>();
        companies.findById(companyId)
                .map(Company::getOwner)
                .ifPresent(staff::add);
        users.findAllByStaffCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .filter(User::isActive)
                .forEach(staff::add);
        String name = users.findById(customerId).map(User::getFullName).orElse("");
        // Each staff member reads it in their own language.
        for (User member : staff) {
            Locale locale = member.getLocale() == null
                    ? Locale.of("az") : member.getLocale().toJavaLocale();
            String who = name.isBlank()
                    ? messages.get("notify.counter.someone", locale) : name;
            push.pushToUsers(List.of(member.getId()),
                    messages.get("notify.counter.title", locale),
                    messages.get("notify.counter.body", locale, who),
                    Map.of("route", "/business/scan", "autoOpen", "1"));
        }
    }

    @Transactional(readOnly = true)
    public UUID openSessionId(UUID companyId, UUID userId) {
        return sessions.findFirstByCompanyIdAndUserIdAndStatus(
                        companyId, userId, SpendSessionStatus.OPEN)
                .map(SpendSession::getId)
                .orElse(null);
    }

    // ─────────────────────── cashier ───────────────────────

    @Transactional(readOnly = true)
    public List<SpendSessionResponse> open(UUID callerId) {
        Company company = access.companyOf(callerId);
        return sessions
                .findAllByCompanyIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                        company.getId(), SpendSessionStatus.OPEN, Instant.now())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public SpendSessionResponse use(UUID callerId, UUID sessionId, String kind, UUID rewardId) {
        SpendSession session = liveSessionForStaff(callerId, sessionId);
        rewards.use(callerId, kind, rewardId, session.getUserId(), session.getCompanyId());
        session.setHandledBy(callerId);
        return toResponse(session);
    }

    @Transactional
    public void close(UUID callerId, UUID sessionId) {
        SpendSession session = sessionForStaff(callerId, sessionId);
        session.setStatus(SpendSessionStatus.COMPLETED);
        session.setHandledBy(callerId);
    }

    private SpendSession liveSessionForStaff(UUID callerId, UUID sessionId) {
        SpendSession session = sessionForStaff(callerId, sessionId);
        if (!session.isLive()) {
            throw new AppException(ErrorCode.SPEND_SESSION_EXPIRED);
        }
        return session;
    }

    private SpendSession sessionForStaff(UUID callerId, UUID sessionId) {
        SpendSession session = sessions.findById(sessionId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
        Company company = companies.findById(session.getCompanyId())
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
        access.requireStaff(company, callerId);
        return session;
    }

    // ─────────────────────── customer ───────────────────────

    @Transactional(readOnly = true)
    public CustomerSpendSessionResponse forCustomer(UUID userId, UUID sessionId) {
        SpendSession session = sessions.findById(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
        Company company = companies.findById(session.getCompanyId())
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
        List<String> used = claims
                .findAllByUserIdAndCompanyIdAndStatusAndUsedAtGreaterThanEqual(
                        userId, company.getId(), RewardClaimStatus.USED,
                        session.getCreatedAt())
                .stream().map(RewardClaim::getTitle).toList();
        String status = session.getStatus() == SpendSessionStatus.OPEN && !session.isLive()
                ? SpendSessionStatus.EXPIRED.name()
                : session.getStatus().name();
        return new CustomerSpendSessionResponse(
                session.getId(), status, company.getName(),
                usableRewards(userId, company.getId()), used);
    }

    // ─────────────────────── helpers ───────────────────────

    private SpendSessionResponse toResponse(SpendSession s) {
        User user = users.findById(s.getUserId())
                .orElseThrow(() -> NotFoundException.of(ErrorCode.USER_NOT_FOUND));
        List<SpendSessionResponse.Card> cardRows = cards
                .findAllByUserIdAndCompanyIdOrderByLastActivityAtDesc(
                        s.getUserId(), s.getCompanyId())
                .stream()
                .map(c -> new SpendSessionResponse.Card(
                        c.getProgram().getTitle(), c.getStamps(),
                        c.getStampsRequired(), c.getRewardsAvailable()))
                .toList();
        return new SpendSessionResponse(
                s.getId(),
                user.getId(),
                user.getFullName(),
                user.getPhone(),
                user.getAvatarUrl(),
                s.getCreatedAt(),
                s.getExpiresAt(),
                ledger.balanceOfAtCompany(s.getUserId(), s.getCompanyId()),
                cardRows,
                usableRewards(s.getUserId(), s.getCompanyId()));
    }

    private List<SpendSessionResponse.Reward> usableRewards(UUID userId, UUID companyId) {
        List<SpendSessionResponse.Reward> out = new ArrayList<>();
        for (RewardClaim c : claims.findAllByUserIdAndCompanyIdAndStatusOrderByCreatedAtDesc(
                userId, companyId, RewardClaimStatus.ACTIVE)) {
            out.add(new SpendSessionResponse.Reward(
                    "COIN", c.getId(), c.getTitle(), c.getCoinCost(),
                    RewardTitles.Info.COIN.rewardType(), null, null, null));
        }
        for (LoyaltyCard card : cards.findAllByUserIdAndCompanyIdOrderByLastActivityAtDesc(
                userId, companyId)) {
            RewardTitles.Info info = rewards.cardRewardInfo(card);
            String title = rewards.cardRewardTitle(card);
            for (int i = 0; i < card.getRewardsAvailable(); i++) {
                out.add(new SpendSessionResponse.Reward(
                        "CARD", card.getId(), title, 0,
                        info.rewardType(), info.rewardValue(),
                        info.rewardItem(), info.currency()));
            }
        }
        return out;
    }
}
