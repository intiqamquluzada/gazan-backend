package az.qazan.backend.user.application;

import az.qazan.backend.user.api.dto.UserResponse;
import az.qazan.backend.user.domain.User;
import org.springframework.stereotype.Component;

/**
 * Single place where the entity → DTO transformation is encoded.
 * Kept hand-written (no MapStruct) — the surface is small and the
 * mapping is trivial.
 */
@Component
public class UserMapper {

    public UserResponse toResponse(User u) {
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.getPhone(),
                u.getAvatarUrl(),
                u.getBusinessName(),
                u.getRole(),
                u.getLocale(),
                u.isActive(),
                u.getCreatedAt(),
                u.getLastLoginAt()
        );
    }
}
