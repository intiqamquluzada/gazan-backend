package az.qazan.backend.admin.auth.dto;

import java.time.Instant;

/**
 * Password accepted; the authenticator code comes next.
 *
 * @param challengeToken pass back to {@code /admin/auth/verify} with the code
 * @param enrolled       false on first sign-in: show {@code setup} as a QR code
 * @param setup          authenticator secret and otpauth URL, only before enrolment
 */
public record AdminLoginResponse(
        String challengeToken,
        Instant challengeExpiresAt,
        boolean enrolled,
        TotpSetup setup
) {
    public record TotpSetup(String secret, String otpauthUrl) {}
}
