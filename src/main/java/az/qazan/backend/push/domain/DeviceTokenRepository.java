package az.qazan.backend.push.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findByUserId(UUID userId);

    List<DeviceToken> findByUserIdIn(Collection<UUID> userIds);

    /** Devices of every customer who holds a loyalty card at the venue. */
    @Query("select distinct dt.token from DeviceToken dt where dt.userId in "
            + "(select c.user.id from LoyaltyCard c where c.company.id = :companyId)")
    List<String> findTokensOfCardholders(@Param("companyId") UUID companyId);

    /** Tokens of the given users who left rewards pushes on. */
    @Query("select distinct dt.token from DeviceToken dt, User u "
            + "where u.id = dt.userId and dt.userId in :userIds and u.notifyRewards = true")
    List<String> findTokensWantingRewards(@Param("userIds") Collection<UUID> userIds);

    /** Cardholders of the venue who left venue-offer pushes on. */
    @Query("select distinct dt.token from DeviceToken dt, User u "
            + "where u.id = dt.userId and u.notifyOffers = true and dt.userId in "
            + "(select c.user.id from LoyaltyCard c where c.company.id = :companyId)")
    List<String> findTokensOfCardholdersWantingOffers(@Param("companyId") UUID companyId);

    /** Everyone who left Qazan announcements on. */
    @Query("select distinct dt.token from DeviceToken dt, User u "
            + "where u.id = dt.userId and u.notifyMarketing = true")
    List<String> findTokensWantingMarketing();

    @Transactional
    @Modifying
    void deleteByToken(String token);

    @Transactional
    @Modifying
    void deleteByUserId(UUID userId);
}
