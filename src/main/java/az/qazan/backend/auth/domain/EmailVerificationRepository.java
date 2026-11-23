package az.qazan.backend.auth.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {

    Optional<EmailVerification> findTopByEmailOrderByCreatedAtDesc(String email);

    /** Locked so parallel guesses are counted one after another. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailVerification>
            findTopByEmailAndVerifiedAtIsNullOrderByCreatedAtDesc(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailVerification> findByTicketHash(String ticketHash);

    /** Drops codes that were never used, so only one is live per address. */
    @Modifying
    @Query("delete from EmailVerification v where v.email = :email and v.consumedAt is null")
    void deleteOpenByEmail(@Param("email") String email);
}
