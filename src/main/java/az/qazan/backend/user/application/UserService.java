package az.qazan.backend.user.application;

import az.qazan.backend.auth.domain.RefreshTokenRepository;
import az.qazan.backend.common.exception.ConflictException;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.NotFoundException;
import az.qazan.backend.common.exception.UnauthorizedException;
import az.qazan.backend.push.domain.DeviceTokenRepository;
import az.qazan.backend.user.api.dto.ChangePasswordRequest;
import az.qazan.backend.user.api.dto.NotificationPrefsRequest;
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
import java.util.Optional;
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
    private final DeviceTokenRepository deviceTokens;
    private final az.qazan.backend.auth.domain.PasswordResetTokenRepository resetTokens;
    private final az.qazan.backend.favorites.domain.FavoriteRepository favorites;
    private final az.qazan.backend.notifications.domain.NotificationReadRepository notificationReads;
    private final az.qazan.backend.reviews.domain.ReviewRepository reviews;
    private final az.qazan.backend.auth.application.AppleTokenService appleTokens;
    private final PasswordEncoder passwordEncoder;

    /** Finds an account by email or phone, whichever was typed. */
    @Transactional(readOnly = true)
    public Optional<User> findByIdentifier(String identifier) {
        String value = identifier.trim();
        if (value.contains("@")) {
            return repository.findByEmailIgnoreCase(value);
        }
        String phone = PhoneNumbers.normalize(value);
        return phone == null
                ? Optional.empty()
                : repository.findFirstByPhoneOrderByCreatedAtAsc(phone);
    }

    @Transactional(readOnly = true)
    public boolean phoneRegistered(String rawPhone) {
        String phone = PhoneNumbers.normalize(rawPhone);
        return phone != null && repository.existsByPhone(phone);
    }

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
        String normalizedPhone = PhoneNumbers.normalize(phone);
        if (normalizedPhone != null && repository.existsByPhone(normalizedPhone)) {
            throw new ConflictException(ErrorCode.AUTH_PHONE_TAKEN);
        }
        User u = User.builder()
                .email(email.trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .fullName(fullName.trim())
                .phone(normalizedPhone)
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
        if (req.phone() != null) applyPhone(u, req.phone());
        if (req.birthDate() != null) u.setBirthDate(req.birthDate());
        if (req.avatarUrl() != null) {
            // An empty value removes the photo.
            String avatar = req.avatarUrl().trim();
            u.setAvatarUrl(avatar.isEmpty() ? null : avatar);
        }
        if (req.businessName() != null) u.setBusinessName(req.businessName().trim());
        if (req.locale() != null) u.setLocale(req.locale());
        return u;
    }

    @Transactional
    public User updateNotificationPrefs(UUID userId, NotificationPrefsRequest req) {
        User u = getById(userId);
        if (req.rewards() != null) u.setNotifyRewards(req.rewards());
        if (req.offers() != null) u.setNotifyOffers(req.offers());
        if (req.weekly() != null) u.setNotifyWeekly(req.weekly());
        if (req.marketing() != null) u.setNotifyMarketing(req.marketing());
        return u;
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest req) {
        User u = getById(userId);
        // Google / Apple accounts have only a random hash: they set their
        // first password without a "current" one (the session proves who
        // they are).
        if (u.isPasswordSet()) {
            String current = req.currentPassword();
            if (current == null || current.isEmpty()
                    || !passwordEncoder.matches(current, u.getPasswordHash())) {
                throw new UnauthorizedException(ErrorCode.USER_PASSWORD_MISMATCH);
            }
        }
        u.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        u.setPasswordSet(true);
        // Signs out every other device; the caller issues this device a
        // fresh session so the user is not thrown out a few minutes later.
        refreshRepository.revokeAllForUser(u, Instant.now());
    }

    private void applyPhone(User u, String rawPhone) {
        String phone = PhoneNumbers.normalize(rawPhone);
        if (phone == null || phone.equals(u.getPhone())) {
            return;
        }
        if (repository.existsByPhoneAndIdNot(phone, u.getId())) {
            throw new ConflictException(ErrorCode.AUTH_PHONE_TAKEN);
        }
        u.setPhone(phone);
        u.setPhoneVerifiedAt(null);
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
        // Sign in with Apple: revoke the Apple session too (guideline 5.1.1(v)).
        appleTokens.revoke(u.getAppleRefreshToken());
        u.setAppleRefreshToken(null);
        refreshRepository.revokeAllForUser(u, Instant.now());
        deviceTokens.deleteByUserId(u.getId());
        // Personal traces beyond the user row itself.
        resetTokens.deleteByUserId(u.getId());
        favorites.deleteAllOfUser(u.getId());
        notificationReads.deleteAllOfUser(u.getId());
        reviews.blankCommentsOf(u.getId());
        u.setEmail("deleted+" + u.getId() + "@qazan.az");
        u.setFullName("Silinmiş istifadəçi");
        u.setPhone(null);
        u.setAvatarUrl(null);
        u.setBusinessName(null);
        u.setBirthDate(null);
        u.setGoogleSub(null);
        u.setAppleSub(null);
        u.setPhoneVerifiedAt(null);
        u.setStaffCompanyId(null);
        u.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
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
        u.setPasswordSet(true);
        refreshRepository.revokeAllForUser(u, Instant.now());
    }

    @Transactional
    public void recordLogin(User u) {
        u.touchLastLogin();
        repository.save(u);
    }
}
