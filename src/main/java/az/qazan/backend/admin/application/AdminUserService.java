package az.qazan.backend.admin.application;

import az.qazan.backend.admin.api.dto.AdminCoinTxnResponse;
import az.qazan.backend.admin.api.dto.AdminUserDetailResponse;
import az.qazan.backend.admin.api.dto.AdminUserResponse;
import az.qazan.backend.admin.api.dto.PageResponse;
import az.qazan.backend.admin.domain.AdminUserRepository;
import az.qazan.backend.coins.domain.CoinTransactionRepository;
import az.qazan.backend.common.exception.BadRequestException;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.NotFoundException;
import az.qazan.backend.loyalty.domain.LoyaltyCardRepository;
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
    private final AdminAuditService audit;
    private final LoyaltyCardRepository cards;
    private final CoinTransactionRepository ledger;

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> list(String q, Role role, int page, int size) {
        Page<User> result = users.search(q, role, PageRequest.of(
                Math.max(page, 0), Math.max(1, Math.min(size, 100))));
        return PageResponse.of(result, AdminUserResponse::from);
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse detail(UUID userId) {
        User u = users.findById(userId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.USER_NOT_FOUND));
        return new AdminUserDetailResponse(
                u.getId(), u.getFullName(), u.getEmail(), u.getPhone(),
                u.getRole().name(), u.isActive(), u.getBirthDate(),
                u.getStaffCompanyId(), u.getCreatedAt(), u.getLastLoginAt(),
                ledger.balanceOf(userId),
                cards.findAllWithRefsByUser(userId).stream()
                        .map(c -> new AdminUserDetailResponse.Card(
                                c.getId(), c.getCompany().getId(), c.getCompany().getName(),
                                c.getProgram().getTitle(), c.getStamps(),
                                c.getStampsRequired(), c.getRewardsAvailable(),
                                c.getTotalRewardsClaimed()))
                        .toList(),
                ledger.findTop15ByUserIdOrderByCreatedAtDesc(userId).stream()
                        .map(AdminCoinTxnResponse::from)
                        .toList());
    }

    @Transactional
    public AdminUserResponse changeRole(UUID actorId, UUID userId, Role role) {
        if (actorId.equals(userId)) {
            // Guard against an admin accidentally locking themselves out.
            throw new BadRequestException(ErrorCode.BAD_REQUEST);
        }
        User u = users.findById(userId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.USER_NOT_FOUND));
        // A cashier always belongs to a business, so that role comes only
        // from the owner's staff screen.
        if (role == Role.CASHIER && u.getStaffCompanyId() == null) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST);
        }
        Role previous = u.getRole();
        u.setRole(role);
        if (role != Role.CASHIER) {
            u.setStaffCompanyId(null);
        }
        audit.record(actorId, "USER_ROLE", "USER", userId, previous + " -> " + role);
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
        audit.record(actorId, "USER_STATUS", "USER", userId, active ? "ACTIVE" : "BLOCKED");
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
            UUID actorId, String email, String password, String fullName,
            String phone, Role role) {
        if (role == Role.CASHIER) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST);
        }
        User created = userService.register(
                email,
                password,
                fullName,
                phone,
                null,
                role == null ? Role.BUSINESS_OWNER : role,
                AppLocale.AZ);
        audit.record(actorId, "USER_CREATE", "USER", created.getId(), created.getRole().name());
        return AdminUserResponse.from(created);
    }

    /**
     * Admin overwrites someone's password — used when a business
     * owner forgets their credentials and contacts support. The user
     * can sign in with the new value immediately.
     */
    @Transactional
    public AdminUserResponse resetPassword(UUID actorId, UUID userId, String newPassword) {
        userService.resetPassword(userId, newPassword);
        audit.record(actorId, "USER_RESET_PASSWORD", "USER", userId, null);
        User u = users.findById(userId)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.USER_NOT_FOUND));
        return AdminUserResponse.from(u);
    }
}
