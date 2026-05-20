package az.qazan.backend.admin.application;

import az.qazan.backend.admin.api.dto.AdminUserResponse;
import az.qazan.backend.admin.api.dto.PageResponse;
import az.qazan.backend.admin.domain.AdminUserRepository;
import az.qazan.backend.common.exception.BadRequestException;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.NotFoundException;
import az.qazan.backend.user.domain.Role;
import az.qazan.backend.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AdminUserRepository users;

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> list(String q, Role role, int page, int size) {
        Page<User> result = users.search(
                q, role, PageRequest.of(page, Math.min(size, 100)));
        return PageResponse.of(result, AdminUserResponse::from);
    }

    @Transactional
    public AdminUserResponse changeRole(UUID actorId, UUID userId, Role role) {
        if (actorId.equals(userId)) {
            // Guard against an admin accidentally locking themselves out.
            throw new BadRequestException(ErrorCode.BAD_REQUEST);
        }
        User u = users.findById(userId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.USER_NOT_FOUND));
        u.setRole(role);
        return AdminUserResponse.from(users.save(u));
    }

    @Transactional
    public AdminUserResponse changeStatus(UUID actorId, UUID userId, boolean active) {
        if (actorId.equals(userId)) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST);
        }
        User u = users.findById(userId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.USER_NOT_FOUND));
        u.setActive(active);
        return AdminUserResponse.from(users.save(u));
    }
}
