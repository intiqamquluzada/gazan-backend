package az.qazan.backend.admin.auth.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * A signed-in web admin session. The refresh token travels only in an
 * httpOnly cookie, never in the body.
 */
public record AdminSessionResponse(
        String accessToken,
        Instant accessExpiresAt,
        long idleTimeoutSeconds,
        AdminProfile admin
) {
    public record AdminProfile(UUID id, String email, String fullName, String avatarUrl,
                               Instant lastLoginAt) {}
}
