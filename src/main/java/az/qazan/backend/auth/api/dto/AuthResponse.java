package az.qazan.backend.auth.api.dto;

import az.qazan.backend.user.api.dto.UserResponse;

import java.time.Instant;

/**
 * Returned by every successful auth flow (register, login, refresh).
 *
 * @param accessToken     Short-lived JWT (HS256)
 * @param accessExpiresAt When the access token stops working
 * @param refreshToken    Long-lived opaque token; rotated on each refresh
 * @param user            The signed-in user's public profile
 */
public record AuthResponse(
        String accessToken,
        Instant accessExpiresAt,
        String refreshToken,
        UserResponse user
) {
}
