package az.qazan.backend.business.application;

import az.qazan.backend.business.api.dto.BusinessCustomerDetailResponse;
import az.qazan.backend.business.api.dto.BusinessCustomerDetailResponse.CardSummary;
import az.qazan.backend.business.api.dto.BusinessCustomerDetailResponse.CustomerEvent;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.NotFoundException;
import az.qazan.backend.companies.application.CompanyService;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.loyalty.domain.LoyaltyCard;
import az.qazan.backend.loyalty.domain.LoyaltyCardRepository;
import az.qazan.backend.loyalty.domain.LoyaltyEvent;
import az.qazan.backend.loyalty.domain.LoyaltyEventRepository;
import az.qazan.backend.user.domain.User;
import az.qazan.backend.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Builds the rich "this customer at my business" view used by the
 * mobile customer-detail screen. Everything is scoped to the
 * owner's own company — an owner can only see customers who hold a
 * loyalty card at their company.
 */
@Service
@RequiredArgsConstructor
public class BusinessCustomerDetailService {

    private final CompanyService companies;
    private final UserRepository users;
    private final LoyaltyCardRepository cards;
    private final LoyaltyEventRepository events;

    @Transactional(readOnly = true)
    public BusinessCustomerDetailResponse detail(UUID ownerId, UUID customerId) {
        Company company = companies.myCompany(ownerId);

        List<LoyaltyCard> myCards = cards
                .findAllByUserIdAndCompanyIdOrderByLastActivityAtDesc(
                        customerId, company.getId());
        if (myCards.isEmpty()) {
            throw new NotFoundException(ErrorCode.NOT_FOUND);
        }
        User u = users.findById(customerId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        int totalStamps = myCards.stream().mapToInt(LoyaltyCard::getStamps).sum();
        int totalClaimed =
                myCards.stream().mapToInt(LoyaltyCard::getTotalRewardsClaimed).sum();
        int available =
                myCards.stream().mapToInt(LoyaltyCard::getRewardsAvailable).sum();
        Instant lastActivity = myCards.stream()
                .map(LoyaltyCard::getLastActivityAt)
                .max(Comparator.naturalOrder())
                .orElse(null);
        Instant memberSince = myCards.stream()
                .map(LoyaltyCard::getCreatedAt)
                .min(Comparator.naturalOrder())
                .orElse(null);

        List<LoyaltyEvent> recent = events.findRecentForUserAtCompany(
                customerId, company.getId(), PageRequest.of(0, 40));
        int visits = (int) recent.stream()
                .filter(e -> e.getType() == LoyaltyEvent.Type.STAMP_ADDED)
                .count();

        List<CardSummary> cardSummaries = myCards.stream()
                .map(c -> new CardSummary(
                        c.getId(),
                        c.getProgram().getId(),
                        c.getProgram().getTitle(),
                        c.getStamps(),
                        c.getStampsRequired(),
                        c.getRewardsAvailable(),
                        c.getTotalRewardsClaimed(),
                        c.getLastActivityAt(),
                        c.getCreatedAt()))
                .toList();

        List<CustomerEvent> activity = recent.stream()
                .map(e -> new CustomerEvent(
                        e.getId(),
                        e.getType().name(),
                        e.getAmount(),
                        e.getNote(),
                        e.getCard().getProgram().getTitle(),
                        e.getCreatedAt()))
                .toList();

        return new BusinessCustomerDetailResponse(
                u.getId(),
                u.getFullName(),
                u.getEmail(),
                u.getPhone(),
                memberSince,
                lastActivity,
                totalStamps,
                totalClaimed,
                available,
                visits,
                myCards.size(),
                cardSummaries,
                activity
        );
    }
}
