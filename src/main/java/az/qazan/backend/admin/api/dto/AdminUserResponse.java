package az.qazan.backend.admin.api.dto;

import az.qazan.backend.user.domain.User;

import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        String role,
        boolean active,
        Instant lastLoginAt,
        Instant createdAt
) {
    public static AdminUserResponse from(User u) {
        return new AdminUserResponse(
                u.getId(),
                u.getFullName(),
                u.getEmail(),
                u.getPhone(),
                u.getRole().name(),
                u.isActive(),
                u.getLastLoginAt(),
                u.getCreatedAt()
        );
    }
}
