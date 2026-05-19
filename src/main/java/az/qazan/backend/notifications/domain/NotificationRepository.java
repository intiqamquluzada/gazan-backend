package az.qazan.backend.notifications.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findTop50ByOrderByCreatedAtDesc();

    @Query("""
        select count(n) from Notification n
         where n.id not in (
             select r.notification.id from NotificationRead r
              where r.userId = :uid)
        """)
    long unreadCount(@Param("uid") UUID userId);

    @Query("""
        select n from Notification n
         where n.id not in (
             select r.notification.id from NotificationRead r
              where r.userId = :uid)
        """)
    List<Notification> findUnreadFor(@Param("uid") UUID userId);
}
