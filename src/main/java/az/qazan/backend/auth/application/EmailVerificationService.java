package az.qazan.backend.auth.application;

import az.qazan.backend.auth.domain.EmailVerification;
import az.qazan.backend.auth.domain.EmailVerificationRepository;
import az.qazan.backend.common.exception.AppException;
import az.qazan.backend.common.exception.BadRequestException;
import az.qazan.backend.common.exception.ConflictException;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.mail.MailService;
import az.qazan.backend.user.domain.AppLocale;
import az.qazan.backend.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Email OTP for password sign-up (customers and business owners):
 *   1. {@link #start} — mails a 6-digit code to an address no account uses yet.
 *   2. {@link #verify} — checks the code and returns a one-time ticket.
 *   3. {@link #consume} — /auth/register spends the ticket for that address.
 * Google / Apple sign-in skips all of this: the provider already proved the
 * address.
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final SecureRandom RNG = new SecureRandom();
    static final Duration CODE_TTL = Duration.ofMinutes(15);
    static final Duration RESEND_GAP = Duration.ofMinutes(1);
    /** Time between a correct code and finishing the sign-up form. */
    static final Duration TICKET_TTL = Duration.ofMinutes(60);
    static final int MAX_ATTEMPTS = 5;

    private final EmailVerificationRepository verifications;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final MailService mail;

    @Transactional
    public void start(String rawEmail, AppLocale locale) {
        String email = normalize(rawEmail);
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException(ErrorCode.AUTH_EMAIL_TAKEN);
        }
        // One mail a minute per address: asking again cannot refill the
        // attempts or flood an inbox. The app shows the same countdown.
        boolean tooSoon = verifications.findTopByEmailOrderByCreatedAtDesc(email)
                .filter(v -> v.getConsumedAt() == null && v.getCreatedAt() != null
                        && v.getCreatedAt().isAfter(Instant.now().minus(RESEND_GAP)))
                .isPresent();
        if (tooSoon) {
            return;
        }
        verifications.deleteOpenByEmail(email);

        String code = String.format("%06d", RNG.nextInt(1_000_000));
        verifications.save(EmailVerification.builder()
                .email(email)
                .codeHash(passwordEncoder.encode(code))
                .expiresAt(Instant.now().plus(CODE_TTL))
                .build());
        mail.sendEmailVerificationCode(email, code, locale);
    }

    /** A wrong guess must still commit its attempt, hence no rollback. */
    @Transactional(noRollbackFor = AppException.class)
    public String verify(String rawEmail, String code) {
        String email = normalize(rawEmail);
        EmailVerification v = verifications
                .findTopByEmailAndVerifiedAtIsNullOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BadRequestException(ErrorCode.AUTH_RESET_CODE_INVALID));
        if (v.getAttempts() >= MAX_ATTEMPTS) {
            throw new AppException(ErrorCode.AUTH_RESET_LOCKED);
        }
        boolean expired = v.getExpiresAt().isBefore(Instant.now());
        if (expired || code == null || !passwordEncoder.matches(code.trim(), v.getCodeHash())) {
            v.setAttempts(v.getAttempts() + 1);
            throw new BadRequestException(ErrorCode.AUTH_RESET_CODE_INVALID);
        }
        byte[] raw = new byte[32];
        RNG.nextBytes(raw);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        v.setVerifiedAt(Instant.now());
        v.setTicketHash(sha256(ticket));
        return ticket;
    }

    /**
     * Spends a ticket for {@code rawEmail}. Runs inside the register
     * transaction, so a failed sign-up leaves the ticket usable.
     */
    @Transactional
    public void consume(String rawEmail, String ticket) {
        if (ticket == null || ticket.isBlank()) {
            throw new BadRequestException(ErrorCode.AUTH_EMAIL_NOT_VERIFIED);
        }
        String email = normalize(rawEmail);
        EmailVerification v = verifications.findByTicketHash(sha256(ticket.trim()))
                .filter(x -> x.getEmail().equals(email))
                .filter(x -> x.getConsumedAt() == null)
                .filter(x -> x.getVerifiedAt() != null
                        && x.getVerifiedAt().isAfter(Instant.now().minus(TICKET_TTL)))
                .orElseThrow(() -> new BadRequestException(ErrorCode.AUTH_EMAIL_NOT_VERIFIED));
        v.setConsumedAt(Instant.now());
    }

    static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
