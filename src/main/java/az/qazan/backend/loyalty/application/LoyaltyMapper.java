package az.qazan.backend.loyalty.application;

import az.qazan.backend.loyalty.api.dto.LoyaltyCardResponse;
import az.qazan.backend.loyalty.api.dto.LoyaltyProgramResponse;
import az.qazan.backend.loyalty.domain.LoyaltyCard;
import az.qazan.backend.loyalty.domain.LoyaltyProgram;
import org.springframework.stereotype.Component;

@Component
public class LoyaltyMapper {

    public LoyaltyProgramResponse toProgramResponse(LoyaltyProgram p) {
        return new LoyaltyProgramResponse(
                p.getId(),
                p.getCompany().getId(),
                p.getTitle(),
                p.getDescription(),
                p.getStampsRequired(),
                p.getRewardType(),
                p.getRewardValue(),
                p.getRewardItem(),
                p.getExpiresAt(),
                p.isActive()
        );
    }

    public LoyaltyCardResponse toCardResponse(LoyaltyCard c) {
        return new LoyaltyCardResponse(
                c.getId(),
                c.getUser().getId(),
                c.getCompany().getId(),
                c.getProgram().getId(),
                c.getStamps(),
                c.getStampsRequired(),
                c.getRewardsAvailable(),
                c.getTotalRewardsClaimed(),
                c.getLastActivityAt()
        );
    }
}
