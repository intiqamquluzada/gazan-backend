package az.qazan.backend.business.api.dto;

import az.qazan.backend.user.domain.User;

import java.time.Instant;
import java.util.UUID;

public record StaffResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        boolean active,
        Instant createdAt
) {
    public static StaffResponse from(User u) {
        return new StaffResponse(
                u.getId(), u.getFullName(), u.getEmail(), u.getPhone(),
                u.isActive(), u.getCreatedAt());
    }
}
