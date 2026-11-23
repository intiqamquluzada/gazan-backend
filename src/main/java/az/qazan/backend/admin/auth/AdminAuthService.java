package az.qazan.backend.admin.auth;

import az.qazan.backend.admin.application.AdminAuditService;
import az.qazan.backend.admin.auth.dto.AdminLoginResponse;
import az.qazan.backend.admin.auth.dto.AdminSessionResponse;
import az.qazan.backend.auth.domain.RefreshToken;
import az.qazan.backend.auth.domain.RefreshTokenRepository;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.UnauthorizedException;
import az.qazan.backend.common.security.JwtTokenProvider;
import az.qazan.backend.user.domain.Role;
import az.qazan.backend.user.domain.User;
import az.qazan.backend.user.domain.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * Web admin panel sign-in: password, then an authenticator code. The first
 * sign-in enrols the authenticator. Sessions slide: each refresh pushes the
 * idle deadline out again, and a session left alone longer simply ends.
 */
@Service
public class AdminAuthService {

    private static final SecureRandom RNG = new SecureRandom();
    private static final String ISSUER = "Qazan Admin";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwt;
    private final TotpService totp;
    private final RefreshTokenRepository refreshRepo;
    private final AdminAuditService audit;
    private final Duration idle;
    private String decoyHash;

    public AdminAuthService(UserRepository users,
                            PasswordEncoder passwordEncoder,
                            JwtTokenProvider jwt,
                            TotpService totp,
                            RefreshTokenRepository refreshRepo,
                            AdminAuditService audit,
                            @Value("${qazan.admin.session-idle:PT30M}") Duration idle) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
        this.totp = totp;
        this.refreshRepo = refreshRepo;
        this.audit = audit;
        this.idle = idle;
    }

    @PostConstruct
    void prepareDecoy() {
        decoyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public Duration idleTimeout() {
        return idle;
    }

    /** Step 1. Non-admin accounts get the same answer as a wrong password. */
    @Transactional
    public AdminLoginResponse login(String email, String password) {
        User user = users.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (user == null) {
            passwordEncoder.matches(password, decoyHash);
            throw new UnauthorizedException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())
                || user.getRole() != Role.ADMIN) {
            throw new UnauthorizedException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        if (!user.isActive()) {
            throw new UnauthorizedException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }

        AdminLoginResponse.TotpSetup setup = null;
        boolean enrolled = user.getTotpEnabledAt() != null && secretOf(user) != null;
        if (!enrolled) {
            String secret = secretOf(user);
            if (secret == null) {
                secret = totp.newSecret();
                user.setTotpSecret(totp.encrypt(secret));
                user.setTotpEnabledAt(null);
                user.setTotpLastStep(null);
                users.save(user);
            }
            setup = new AdminLoginResponse.TotpSetup(
                    secret, totp.otpauthUrl(ISSUER, user.getEmail(), secret));
        }
        JwtTokenProvider.IssuedToken challenge = jwt.issueAdminChallenge(user.getId());
        return new AdminLoginResponse(challenge.value(), challenge.expiresAt(), enrolled, setup);
    }

    /** Step 2. A good code opens the session (and completes enrolment the first time). */
    @Transactional
    public Session verify(String challengeToken, String code, HttpServletRequest http) {
        User user = users.findById(jwt.parseAdminChallenge(challengeToken))
                .filter(u -> u.getRole() == Role.ADMIN && u.isActive())
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID));
        OptionalLong step = totp.verify(secretOf(user), code, user.getTotpLastStep());
        if (step.isEmpty()) {
            throw new UnauthorizedException(ErrorCode.ADMIN_TOTP_INVALID);
        }
        boolean firstTime = user.getTotpEnabledAt() == null;
        user.setTotpLastStep(step.getAsLong());
        if (firstTime) user.setTotpEnabledAt(Instant.now());
        user.touchLastLogin();
        users.save(user);
        audit.record(user.getId(), firstTime ? "ADMIN_TOTP_ENROLLED" : "ADMIN_SIGN_IN",
                "USER", user.getId(), ip(http));
        return open(user, http);
    }

    @Transactional
    public Session refresh(String cookieValue, HttpServletRequest http) {
        RefreshToken token = cookieValue == null ? null
                : refreshRepo.findByTokenValue(cookieValue).orElse(null);
        if (token == null || !token.isActive()
                || !RefreshToken.CHANNEL_ADMIN_WEB.equals(token.getChannel())) {
            throw new UnauthorizedException(ErrorCode.AUTH_REFRESH_INVALID);
        }
        token.revoke();
        User user = token.getUser();
        if (!user.isActive() || user.getRole() != Role.ADMIN) {
            throw new UnauthorizedException(ErrorCode.AUTH_REFRESH_INVALID);
        }
        return open(user, http);
    }

    @Transactional
    public void logout(String cookieValue) {
        if (cookieValue == null) return;
        refreshRepo.findByTokenValue(cookieValue)
                .filter(t -> RefreshToken.CHANNEL_ADMIN_WEB.equals(t.getChannel()))
                .ifPresent(RefreshToken::revoke);
    }

    @Transactional(readOnly = true)
    public AdminSessionResponse.AdminProfile profile(UUID id) {
        return users.findById(id).map(AdminAuthService::profileOf)
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.UNAUTHORIZED));
    }

    private Session open(User user, HttpServletRequest http) {
        JwtTokenProvider.IssuedToken access =
                jwt.issueAccessToken(user.getId(), user.getEmail(), user.getRole(), true);
        byte[] bytes = new byte[48];
        RNG.nextBytes(bytes);
        RefreshToken refresh = refreshRepo.save(RefreshToken.builder()
                .tokenValue(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes))
                .user(user)
                .channel(RefreshToken.CHANNEL_ADMIN_WEB)
                .expiresAt(Instant.now().plus(idle))
                .userAgent(truncate(http == null ? null : http.getHeader("User-Agent"), 256))
                .ip(truncate(ip(http), 64))
                .build());
        return new Session(new AdminSessionResponse(
                access.value(), access.expiresAt(), idle.toSeconds(), profileOf(user)),
                refresh.getTokenValue());
    }

    private String secretOf(User user) {
        return user.getTotpSecret() == null ? null : totp.decrypt(user.getTotpSecret());
    }

    private static AdminSessionResponse.AdminProfile profileOf(User u) {
        return new AdminSessionResponse.AdminProfile(
                u.getId(), u.getEmail(), u.getFullName(), u.getAvatarUrl(), u.getLastLoginAt());
    }

    private static String ip(HttpServletRequest req) {
        if (req == null) return null;
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",", 2)[0].trim();
        }
        return req.getRemoteAddr();
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    /** Body for the browser plus the refresh token the controller puts in a cookie. */
    public record Session(AdminSessionResponse body, String refreshToken) {}
}
