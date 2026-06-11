package az.qazan.backend.auth.application;

import az.qazan.backend.auth.api.dto.AuthResponse;
import az.qazan.backend.auth.api.dto.LoginRequest;
import az.qazan.backend.auth.api.dto.RegisterRequest;
import az.qazan.backend.auth.domain.RefreshToken;
import az.qazan.backend.auth.domain.RefreshTokenRepository;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.UnauthorizedException;
import az.qazan.backend.common.security.JwtProperties;
import az.qazan.backend.common.security.JwtTokenProvider;
import az.qazan.backend.user.application.UserMapper;
import az.qazan.backend.user.application.UserService;
import az.qazan.backend.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

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

    @Transactional
    public AuthResponse register(RegisterRequest req, HttpServletRequest http) {
        User user = userService.register(
                req.email(),
                req.password(),
                req.fullName(),
                req.phone(),
                req.birthDate(),
                req.role(),
                req.locale()
        );
        return issueTokens(user, http);
    }

    @Transactional
    public AuthResponse login(LoginRequest req, HttpServletRequest http) {
        User user = userService.getByEmail(req.email());
        if (!user.isActive()) {
            throw new UnauthorizedException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new UnauthorizedException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        userService.recordLogin(user);
        return issueTokens(user, http);
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
