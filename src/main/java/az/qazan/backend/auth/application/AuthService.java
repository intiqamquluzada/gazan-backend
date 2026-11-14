package az.qazan.backend.auth.application;

import az.qazan.backend.auth.api.dto.AuthResponse;
import az.qazan.backend.auth.api.dto.LoginRequest;
import az.qazan.backend.auth.api.dto.RegisterRequest;
import az.qazan.backend.auth.api.dto.SocialLoginRequest;
import az.qazan.backend.auth.domain.RefreshToken;
import az.qazan.backend.auth.domain.RefreshTokenRepository;
import az.qazan.backend.common.exception.BadRequestException;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.UnauthorizedException;
import az.qazan.backend.common.security.JwtProperties;
import az.qazan.backend.common.security.JwtTokenProvider;
import az.qazan.backend.user.application.PhoneNumbers;
import az.qazan.backend.user.application.UserMapper;
import az.qazan.backend.user.application.UserService;
import az.qazan.backend.user.domain.AppLocale;
import az.qazan.backend.user.domain.Role;
import az.qazan.backend.user.domain.User;
import az.qazan.backend.user.domain.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates register / login / refresh / logout. Issues access JWTs
 * via {@link JwtTokenProvider} and persists rotating refresh tokens.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom RNG = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER =
            Base64.getUrlEncoder().withoutPadding();

    private final UserService userService;
    private final UserMapper userMapper;
    private final RefreshTokenRepository refreshRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwt;
    private final JwtProperties jwtProperties;
    private final UserRepository users;
    private final SocialTokenVerifier socialTokens;

    /** Spent on unknown accounts so they take as long as a wrong password. */
    private String decoyHash;

    @PostConstruct
    void prepareDecoy() {
        decoyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public AuthResponse register(RegisterRequest req, HttpServletRequest http) {
        Role role = req.role() == null ? Role.CUSTOMER : req.role();
        if (role != Role.CUSTOMER && role != Role.BUSINESS_OWNER) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST);
        }
        if (role == Role.CUSTOMER && PhoneNumbers.normalize(req.phone()) == null) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST);
        }
        User user = userService.register(
                req.email(),
                req.password(),
                req.fullName(),
                req.phone(),
                req.birthDate(),
                role,
                req.locale()
        );
        return issueTokens(user, http);
    }

    @Transactional
    public AuthResponse login(LoginRequest req, HttpServletRequest http) {
        String identifier = req.identifier() != null && !req.identifier().isBlank()
                ? req.identifier() : req.email();
        User user = identifier == null || identifier.isBlank()
                ? null
                : userService.findByIdentifier(identifier).orElse(null);
        if (user == null) {
            passwordEncoder.matches(req.password(), decoyHash);
            throw new UnauthorizedException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new UnauthorizedException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        if (!user.isActive()) {
            throw new UnauthorizedException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }
        userService.recordLogin(user);
        return issueTokens(user, http);
    }

    /** Signs in with a verified Google or Apple token, creating the account if new. */
    @Transactional
    public AuthResponse socialLogin(SocialLoginRequest req, HttpServletRequest http) {
        boolean apple = "APPLE".equals(req.provider());
        SocialIdentity identity = socialTokens.verify(req.provider(), req.idToken());
        String email = identity.emailVerified() ? identity.email() : null;

        User user = (apple
                ? users.findByAppleSub(identity.subject())
                : users.findByGoogleSub(identity.subject()))
                .or(() -> email == null
                        ? Optional.empty()
                        : users.findByEmailIgnoreCase(email))
                .orElseGet(() -> newSocialUser(identity, email, req.fullName(), apple));

        if (!user.isActive()) {
            throw new UnauthorizedException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }
        if (apple) {
            user.setAppleSub(identity.subject());
        } else {
            user.setGoogleSub(identity.subject());
        }
        userService.recordLogin(user);
        return issueTokens(user, http);
    }

    private User newSocialUser(SocialIdentity identity, String email,
                               String requestedName, boolean apple) {
        String name = firstNonBlank(requestedName, identity.name(),
                email == null ? null : email.substring(0, email.indexOf('@')), "Qazan");
        return users.save(User.builder()
                .email(email != null
                        ? email.trim().toLowerCase()
                        : (apple ? "apple." : "google.") + identity.subject() + "@social.qazan.az")
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .fullName(name.length() > 120 ? name.substring(0, 120) : name.trim())
                .role(Role.CUSTOMER)
                .locale(AppLocale.AZ)
                .active(true)
                .build());
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return "";
    }

    @Transactional
    public AuthResponse refresh(String refreshTokenValue, HttpServletRequest http) {
        RefreshToken token = refreshRepo.findByTokenValue(refreshTokenValue)
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.AUTH_REFRESH_INVALID));
        if (!token.isActive()) {
            throw new UnauthorizedException(ErrorCode.AUTH_REFRESH_INVALID);
        }
        // Rotation: revoke the presented token and issue a fresh pair.
        token.revoke();
        User user = token.getUser();
        if (!user.isActive()) {
            throw new UnauthorizedException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }
        return issueTokens(user, http);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        refreshRepo.findByTokenValue(refreshTokenValue)
                .ifPresent(RefreshToken::revoke);
    }

    @Transactional
    public void logoutAllSessions(User user) {
        refreshRepo.revokeAllForUser(user, Instant.now());
    }

    // ─────────────────────────── helpers ───────────────────────────

    private AuthResponse issueTokens(User user, HttpServletRequest http) {
        JwtTokenProvider.IssuedToken access =
                jwt.issueAccessToken(user.getId(), user.getEmail(), user.getRole());
        RefreshToken refresh = persistRefreshToken(user, http);
        return new AuthResponse(
                access.value(),
                access.expiresAt(),
                refresh.getTokenValue(),
                userMapper.toResponse(user)
        );
    }

    private RefreshToken persistRefreshToken(User user, HttpServletRequest http) {
        byte[] bytes = new byte[48];
        RNG.nextBytes(bytes);
        String value = URL_ENCODER.encodeToString(bytes);

        RefreshToken token = RefreshToken.builder()
                .tokenValue(value)
                .user(user)
                .expiresAt(Instant.now().plus(jwtProperties.refreshTokenTtl()))
                .userAgent(http == null ? null : truncate(http.getHeader("User-Agent"), 256))
                .ip(http == null ? null : truncate(extractIp(http), 64))
                .build();
        return refreshRepo.save(token);
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String extractIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",", 2)[0].trim();
        }
        return req.getRemoteAddr();
    }
}
