package az.qazan.backend.business.domain;

import az.qazan.backend.loyalty.domain.LoyaltyCard;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Read-only aggregation over loyalty cards, scoped to one company. Kept
 * as its own repository (rather than extending the loyalty module's) so
 * the business feature stays self-contained and additive.
 */
public interface BusinessCustomerRepository
        extends Repository<LoyaltyCard, UUID> {

    @Query("""
        select u.id                            as userId,
               u.fullName                      as fullName,
               u.phone                         as phone,
               coalesce(sum(c.stamps), 0)      as totalStamps,
               coalesce(sum(c.totalRewardsClaimed), 0) as rewardsClaimed,
               count(c.id)                     as cardCount,
               max(c.lastActivityAt)           as lastActivityAt
          from LoyaltyCard c
          join c.user u
         where c.company.id = :companyId
         group by u.id, u.fullName, u.phone
         order by max(c.lastActivityAt) desc
        """)
    List<BusinessCustomerProjection> customersOfCompany(
            @Param("companyId") UUID companyId);
}
