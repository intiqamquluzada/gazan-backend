package az.qazan.backend.user.application;

import az.qazan.backend.auth.domain.RefreshTokenRepository;
import az.qazan.backend.common.exception.ConflictException;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.NotFoundException;
import az.qazan.backend.common.exception.UnauthorizedException;
import az.qazan.backend.user.api.dto.ChangePasswordRequest;
import az.qazan.backend.user.api.dto.UpdateProfileRequest;
import az.qazan.backend.user.domain.AppLocale;
import az.qazan.backend.user.domain.Role;
import az.qazan.backend.user.domain.User;
import az.qazan.backend.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * All write paths against the {@link User} aggregate. Read paths are
 * also funnelled here so we have one place to add caching later.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final RefreshTokenRepository refreshRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> NotFoundException.of(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public User register(String email,
                         String rawPassword,
                         String fullName,
                         String phone,
                         LocalDate birthDate,
                         Role role,
                         AppLocale locale) {
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException(ErrorCode.AUTH_EMAIL_TAKEN);
        }
        User u = User.builder()
                .email(email.trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .fullName(fullName.trim())
                .phone(phone)
                .birthDate(birthDate)
                .role(role == null ? Role.CUSTOMER : role)
                .locale(locale == null ? AppLocale.AZ : locale)
                .active(true)
                .build();
        return repository.save(u);
    }

    @Transactional
    public User updateProfile(UUID userId, UpdateProfileRequest req) {
        User u = getById(userId);
        if (req.fullName() != null) u.setFullName(req.fullName().trim());
        if (req.phone() != null) u.setPhone(req.phone().trim());
        if (req.avatarUrl() != null) u.setAvatarUrl(req.avatarUrl().trim());
        if (req.businessName() != null) u.setBusinessName(req.businessName().trim());
        if (req.locale() != null) u.setLocale(req.locale());
        return u;
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest req) {
        User u = getById(userId);
        if (!passwordEncoder.matches(req.currentPassword(), u.getPasswordHash())) {
            throw new UnauthorizedException(ErrorCode.USER_PASSWORD_MISMATCH);
        }
        u.setPasswordHash(passwordEncoder.encode(req.newPassword()));
    }

    /**
     * Permanently deletes the account in a referential-integrity-safe way:
     * every session is revoked and all personally identifiable data is
     * stripped (GDPR / Apple App Store guideline 5.1.1(v)). The row itself
     * is retained — anonymized — so ledger/audit foreign keys (coins,
     * reward claims, loyalty cards) stay intact and the original email is
     * freed for re-registration.
     */
    @Transactional
    public void deactivate(UUID userId) {
        User u = getById(userId);
        refreshRepository.revokeAllForUser(u, Instant.now());
        u.setEmail("deleted+" + u.getId() + "@qazan.az");
        u.setFullName("Silinmiş istifadəçi");
        u.setPhone(null);
        u.setAvatarUrl(null);
        u.setBusinessName(null);
        u.setBirthDate(null);
        u.setActive(false);
    }

    /**
     * Admin reset — overwrites the password hash without requiring
     * the current one. Used by the admin's "reset password" action so
     * a business owner can sign in with the new value while their
     * previous one is invalidated immediately.
     */
    @Transactional
    public void resetPassword(UUID userId, String newRawPassword) {
        User u = getById(userId);
        u.setPasswordHash(passwordEncoder.encode(newRawPassword));
    }

    @Transactional
    public void recordLogin(User u) {
        u.touchLastLogin();
        repository.save(u);
    }
}
