package az.qazan.backend.admin.application;

import az.qazan.backend.admin.api.dto.AdminUserResponse;
import az.qazan.backend.admin.api.dto.PageResponse;
import az.qazan.backend.admin.domain.AdminUserRepository;
import az.qazan.backend.common.exception.BadRequestException;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.NotFoundException;
import az.qazan.backend.user.application.UserService;
import az.qazan.backend.user.domain.AppLocale;
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
    private final UserService userService;

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

    /**
     * Admin creates a new account directly — handy for onboarding a
     * business owner who can't go through the normal sign-up flow.
     * Uses {@link UserService#register} so duplicate-email checks and
     * password hashing happen the usual way.
     */
    @Transactional
    public AdminUserResponse create(
            String email, String password, String fullName, String phone, Role role) {
        User created = userService.register(
                email,
                password,
                fullName,
                phone,
                role == null ? Role.BUSINESS_OWNER : role,
                AppLocale.AZ);
        return AdminUserResponse.from(created);
    }

    /**
     * Admin overwrites someone's password — used when a business
     * owner forgets their credentials and contacts support. The user
     * can sign in with the new value immediately.
     */
    @Transactional
    public AdminUserResponse resetPassword(UUID userId, String newPassword) {
        userService.resetPassword(userId, newPassword);
        User u = users.findById(userId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.USER_NOT_FOUND));
        return AdminUserResponse.from(u);
    }
}
