package az.qazan.backend.user.api.dto;

import az.qazan.backend.user.domain.AppLocale;
import az.qazan.backend.user.domain.Role;

import java.time.Instant;
import java.time.LocalDate;
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
        LocalDate birthDate,
        String avatarUrl,
        String businessName,
        Role role,
        AppLocale locale,
        boolean active,
        Instant createdAt,
        Instant lastLoginAt,
        /** False until a phone number is on file (e.g. after social sign-in). */
        boolean profileComplete,
        UUID staffCompanyId,
        /** False for Google / Apple accounts that never set a password. */
        boolean hasPassword,
        NotificationPrefs notificationPrefs
) {
    public record NotificationPrefs(
            boolean rewards, boolean offers, boolean weekly, boolean marketing) {
    }
}
