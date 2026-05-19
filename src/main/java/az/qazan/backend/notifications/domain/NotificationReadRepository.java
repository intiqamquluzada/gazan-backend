package az.qazan.backend.notifications.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;
import java.util.UUID;

public interface NotificationReadRepository
        extends JpaRepository<NotificationRead, UUID> {

    @Query("""
        select r.notification.id from NotificationRead r
         where r.userId = :uid
        """)
    Set<UUID> readNotificationIds(@Param("uid") UUID userId);
}
