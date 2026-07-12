package az.qazan.backend.notifications.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /** Admin sent list — every approved row, newest first. */
    @Query("""
        select n from Notification n
         where n.status = az.qazan.backend.notifications.domain.NotificationStatus.APPROVED
         order by n.createdAt desc
        """)
    List<Notification> findApprovedRecent(org.springframework.data.domain.Pageable page);

    /** The admin's moderation queue: notifications waiting for approval. */
    @Query("""
        select n from Notification n
         where n.status = az.qazan.backend.notifications.domain.NotificationStatus.PENDING
         order by n.createdAt asc
        """)
    List<Notification> findPending();

    /**
     * A user's inbox. Returns every APPROVED notification that targets
     * them — broadcast, direct, or via a loyalty card at the targeted
     * company.
     */
    @Query("""
        select n from Notification n
         where n.status = az.qazan.backend.notifications.domain.NotificationStatus.APPROVED
           and (
                n.targetType = az.qazan.backend.notifications.domain.NotificationTarget.BROADCAST
             or (n.targetType = az.qazan.backend.notifications.domain.NotificationTarget.USER
                 and n.userId = :uid)
             or (n.targetType = az.qazan.backend.notifications.domain.NotificationTarget.COMPANY_CARDHOLDERS
                 and n.targetCompanyId in (
                       select lc.company.id from LoyaltyCard lc where lc.user.id = :uid))
           )
         order by n.createdAt desc
        """)
    List<Notification> inboxFor(@Param("uid") UUID userId);

    @Query("""
        select count(n) from Notification n
         where n.status = az.qazan.backend.notifications.domain.NotificationStatus.APPROVED
           and (
                n.targetType = az.qazan.backend.notifications.domain.NotificationTarget.BROADCAST
             or (n.targetType = az.qazan.backend.notifications.domain.NotificationTarget.USER
                 and n.userId = :uid)
             or (n.targetType = az.qazan.backend.notifications.domain.NotificationTarget.COMPANY_CARDHOLDERS
                 and n.targetCompanyId in (
                       select lc.company.id from LoyaltyCard lc where lc.user.id = :uid))
           )
           and n.id not in (
             select r.notification.id from NotificationRead r
              where r.userId = :uid)
        """)
    long unreadCount(@Param("uid") UUID userId);

    @Query("""
        select n from Notification n
         where n.status = az.qazan.backend.notifications.domain.NotificationStatus.APPROVED
           and (
                n.targetType = az.qazan.backend.notifications.domain.NotificationTarget.BROADCAST
             or (n.targetType = az.qazan.backend.notifications.domain.NotificationTarget.USER
                 and n.userId = :uid)
             or (n.targetType = az.qazan.backend.notifications.domain.NotificationTarget.COMPANY_CARDHOLDERS
                 and n.targetCompanyId in (
                       select lc.company.id from LoyaltyCard lc where lc.user.id = :uid))
           )
           and n.id not in (
             select r.notification.id from NotificationRead r
              where r.userId = :uid)
        """)
    List<Notification> findUnreadFor(@Param("uid") UUID userId);
}
