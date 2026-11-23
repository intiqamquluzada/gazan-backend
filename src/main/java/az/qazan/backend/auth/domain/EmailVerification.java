package az.qazan.backend.auth.domain;

import az.qazan.backend.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Proof that someone controls an email address before an account is made
 * for it. The 6-digit code and the ticket handed out after a correct code
 * are only stored hashed.
 */
@Entity
@Table(name = "email_verifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerification extends BaseEntity {

    /** Lower-cased address the code was sent to. */
    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Wrong guesses so far; the code dies after five. */
    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    /** SHA-256 (hex) of the ticket returned after a correct code. */
    @Column(name = "ticket_hash", length = 64)
    private String ticketHash;

    /** Set when /auth/register used the ticket; it is single-use. */
    @Column(name = "consumed_at")
    private Instant consumedAt;
}
