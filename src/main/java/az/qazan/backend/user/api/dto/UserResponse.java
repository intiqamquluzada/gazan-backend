package az.qazan.backend.user.api.dto;

import az.qazan.backend.user.domain.AppLocale;
import az.qazan.backend.user.domain.Role;

import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a user. Never includes the password hash
 * or any other secret.
 */
public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        String avatarUrl,
        String businessName,
        Role role,
        AppLocale locale,
        boolean active,
        Instant createdAt,
        Instant lastLoginAt
) {
}
