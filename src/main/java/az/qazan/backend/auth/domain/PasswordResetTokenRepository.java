package az.qazan.backend.auth.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, UUID> {

    /** Locked so parallel guesses are counted one after another. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetToken>
            findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(UUID userId);

    @Modifying
    @Query("delete from PasswordResetToken t where t.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
