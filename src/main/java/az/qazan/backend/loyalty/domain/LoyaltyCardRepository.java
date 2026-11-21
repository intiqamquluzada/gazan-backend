package az.qazan.backend.loyalty.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoyaltyCardRepository extends JpaRepository<LoyaltyCard, UUID> {

    List<LoyaltyCard> findAllByUserIdOrderByLastActivityAtDesc(UUID userId);

    /** Completed cards waiting to be handed out at one venue. */
    @org.springframework.data.jpa.repository.Query("""
            select c from LoyaltyCard c
            where c.company.id = :companyId and c.rewardsAvailable > 0
            order by c.lastActivityAt desc
            """)
    List<LoyaltyCard> findPendingAtCompany(
            @org.springframework.data.repository.query.Param("companyId") UUID companyId);

    Optional<LoyaltyCard> findByUserIdAndProgramId(UUID userId, UUID programId);

    int countByCompanyId(UUID companyId);

    boolean existsByProgramIdAndRewardsAvailableGreaterThan(UUID programId, int rewards);

    boolean existsByCompanyIdAndRewardsAvailableGreaterThan(UUID companyId, int rewards);

    /** Every card a customer holds, with venue and campaign loaded. */
    @org.springframework.data.jpa.repository.Query("""
            select c from LoyaltyCard c
            join fetch c.company join fetch c.program
            where c.user.id = :userId
            order by c.lastActivityAt desc
            """)
    List<LoyaltyCard> findAllWithRefsByUser(
            @org.springframework.data.repository.query.Param("userId") UUID userId);

    @org.springframework.data.jpa.repository.Query("""
            select count(distinct c.user.id) from LoyaltyCard c
            where c.company.id = :companyId
            """)
    long countCustomers(
            @org.springframework.data.repository.query.Param("companyId") UUID companyId);

    /** Customers whose first card at this venue was created in the window. */
    @org.springframework.data.jpa.repository.Query("""
            select count(distinct c.user.id) from LoyaltyCard c
            where c.company.id = :companyId
              and c.user.id not in (
                    select o.user.id from LoyaltyCard o
                    where o.company.id = :companyId and o.createdAt < :since)
            """)
    long countNewCustomers(
            @org.springframework.data.repository.query.Param("companyId") UUID companyId,
            @org.springframework.data.repository.query.Param("since") java.time.Instant since);

    @org.springframework.data.jpa.repository.Query("""
            select c.program.id as programId,
                   count(c) as members,
                   coalesce(sum(c.rewardsAvailable + c.totalRewardsClaimed), 0) as completed,
                   coalesce(sum(c.totalRewardsClaimed), 0) as redeemed
            from LoyaltyCard c
            where c.company.id = :companyId
            group by c.program.id
            """)
    List<ProgramCardStats> programStats(
            @org.springframework.data.repository.query.Param("companyId") UUID companyId);

    /**
     * Every card a customer holds at one company. Used by the business
     * dashboard's customer-detail view (one row per loyalty program).
     */
    List<LoyaltyCard> findAllByUserIdAndCompanyIdOrderByLastActivityAtDesc(
            UUID userId, UUID companyId);
}
