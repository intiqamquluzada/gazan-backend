package az.qazan.backend.auth.application;

import az.qazan.backend.auth.domain.PasswordResetToken;
import az.qazan.backend.auth.domain.PasswordResetTokenRepository;
import az.qazan.backend.auth.domain.RefreshTokenRepository;
import az.qazan.backend.common.exception.AppException;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.BadRequestException;
import az.qazan.backend.common.mail.MailService;
import az.qazan.backend.user.domain.User;
import az.qazan.backend.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

/**
 * Email-code password reset:
 *   1. {@link #request} — emails a 6-digit code (always succeeds, even for
 *      unknown emails, to avoid leaking which addresses are registered).
 *   2. {@link #confirm} — verifies the code and sets the new password,
 *      then revokes every existing session.
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom RNG = new SecureRandom();
    private static final Duration TTL = Duration.ofMinutes(15);
    private static final Duration RESEND_GAP = Duration.ofMinutes(1);
    private static final int MAX_ATTEMPTS = 5;

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final MailService mail;

    /**
     * Emails a reset code. Product decision (2026-11-15): tell the user when
     * no active account uses this email instead of a silent success — the
     * auth rate limit keeps address probing slow.
     */
    @Transactional
    public void request(String email) {
        az.qazan.backend.user.domain.User found = users.findByEmailIgnoreCase(email.trim())
                .filter(az.qazan.backend.user.domain.User::isActive)
                .orElseThrow(() -> new az.qazan.backend.common.exception.AppException(
                        az.qazan.backend.common.exception.ErrorCode.AUTH_EMAIL_NOT_REGISTERED));
        java.util.Optional.of(found).ifPresent(user -> {
            // One code a minute, so asking again cannot refill the attempts.
            boolean tooSoon = tokens
                    .findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId())
                    .filter(t -> t.getCreatedAt() != null
                            && t.getCreatedAt().isAfter(Instant.now().minus(RESEND_GAP)))
                    .isPresent();
            if (tooSoon) {
                return;
            }
            // Only one live code per user.
            tokens.deleteByUserId(user.getId());

            String code = String.format("%06d", RNG.nextInt(1_000_000));
            tokens.save(PasswordResetToken.builder()
                    .userId(user.getId())
                    .codeHash(passwordEncoder.encode(code))
                    .expiresAt(Instant.now().plus(TTL))
                    .build());

            mail.sendPasswordResetCode(user.getEmail(), code, user.getLocale());
        });
        // No-op for unknown emails — response is identical either way.
    }

    /** A wrong guess must still commit its attempt, hence no rollback. */
    @Transactional(noRollbackFor = AppException.class)
    public void confirm(String email, String code, String newPassword) {
        User user = users.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new BadRequestException(ErrorCode.AUTH_RESET_CODE_INVALID));

        PasswordResetToken token = tokens
                .findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new BadRequestException(ErrorCode.AUTH_RESET_CODE_INVALID));

        if (token.getAttempts() >= MAX_ATTEMPTS) {
            throw new AppException(ErrorCode.AUTH_RESET_LOCKED);
        }
        boolean expired = token.getExpiresAt().isBefore(Instant.now());
        if (expired || !passwordEncoder.matches(code, token.getCodeHash())) {
            token.setAttempts(token.getAttempts() + 1);
            throw new BadRequestException(ErrorCode.AUTH_RESET_CODE_INVALID);
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        token.setUsedAt(Instant.now());
        // Force re-login everywhere after a reset.
        refreshTokens.revokeAllForUser(user, Instant.now());
    }
}
