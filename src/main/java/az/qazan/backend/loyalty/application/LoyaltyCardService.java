package az.qazan.backend.loyalty.application;

import az.qazan.backend.common.exception.AppException;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.NotFoundException;
import az.qazan.backend.notifications.application.NotificationService;
import az.qazan.backend.companies.application.BusinessAccess;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.loyalty.domain.LoyaltyCard;
import az.qazan.backend.loyalty.domain.LoyaltyCardRepository;
import az.qazan.backend.loyalty.domain.LoyaltyEvent;
import az.qazan.backend.loyalty.domain.LoyaltyEventRepository;
import az.qazan.backend.loyalty.domain.LoyaltyProgram;
import az.qazan.backend.user.domain.User;
import az.qazan.backend.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoyaltyCardService {

    private final LoyaltyCardRepository cards;
    private final LoyaltyEventRepository events;
    private final LoyaltyProgramService programs;
    private final UserRepository users;
    private final BusinessAccess access;
    private final NotificationService notifications;

    @Transactional(readOnly = true)
    public List<LoyaltyCard> myCards(UUID userId) {
        return cards.findAllByUserIdOrderByLastActivityAtDesc(userId);
    }

    @Transactional
    public LoyaltyCard joinProgram(UUID userId, UUID programId) {
        LoyaltyProgram program = programs.getById(programId);
        if (!program.isOpen()) {
            throw new AppException(ErrorCode.PROGRAM_CLOSED);
        }
        return cards.findByUserIdAndProgramId(userId, programId)
                .orElseGet(() -> {
                    User user = users.findById(userId)
                            .orElseThrow(() -> NotFoundException.of(ErrorCode.USER_NOT_FOUND));
                    LoyaltyCard card = LoyaltyCard.builder()
                            .user(user)
                            .program(program)
                            .company(program.getCompany())
                            .stamps(0)
                            .stampsRequired(program.getStampsRequired())
                            .rewardsAvailable(0)
                            .totalRewardsClaimed(0)
                            .lastActivityAt(Instant.now())
                            .build();
                    return cards.save(card);
                });
    }

    @Transactional
    public LoyaltyCard addStamp(UUID cardId, int amount, String note) {
        LoyaltyCard card = cards.findById(cardId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
        card.addStamp(amount);
        events.save(LoyaltyEvent.builder()
                .card(card)
                .type(LoyaltyEvent.Type.STAMP_ADDED)
                .amount(amount)
                .note(note)
                .build());
        return card;
    }

    /**
     * Staff add N stamps to a customer's card by hand.
     * Caller must be the owner or a cashier of the company running the program.
     */
    @Transactional
    public LoyaltyCard scan(UUID businessOwnerId,
                            UUID customerId,
                            UUID programId,
                            int amount,
                            String note) {
        LoyaltyProgram program = programs.getById(programId);
        Company company = program.getCompany();
        access.requireStaff(company, businessOwnerId);
        LoyaltyCard card = joinProgram(customerId, programId);
        LoyaltyCard stamped = addStamp(card.getId(), amount, note);
        // A hand-added stamp has no tap celebration, so tell the customer.
        notifications.notifyUserLocalized(customerId, "notify.stamp_added",
                java.util.Map.of("route", "/cards"),
                amount, company.getName(), stamped.getStamps(), stamped.getStampsRequired());
        return stamped;
    }

    @Transactional
    public LoyaltyCard redeem(UUID userId, UUID cardId) {
        LoyaltyCard card = cards.findById(cardId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.NOT_FOUND));
        if (!card.getUser().getId().equals(userId)) {
            throw new NotFoundException(ErrorCode.NOT_FOUND);
        }
        card.claimOneReward();
        events.save(LoyaltyEvent.builder()
                .card(card)
                .type(LoyaltyEvent.Type.REWARD_CLAIMED)
                .amount(1)
                .build());
        return card;
    }
}
