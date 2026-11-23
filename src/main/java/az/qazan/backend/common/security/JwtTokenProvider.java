package az.qazan.backend.common.security;

import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.exception.UnauthorizedException;
import az.qazan.backend.user.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and verifies short-lived access tokens. Refresh tokens are
 * opaque random strings persisted in the database, not JWTs.
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE  = "role";
    /** Set on tokens of the web admin panel, issued after the authenticator code. */
    public static final String CLAIM_MFA = "mfa";
    /** Issuer suffix of the short sign-in challenge, so it never works as an access token. */
    private static final String CHALLENGE_SUFFIX = "#admin-mfa";
    private static final java.time.Duration CHALLENGE_TTL = java.time.Duration.ofMinutes(5);
    private static final String DEV_SECRET_PREFIX = "dev-only-secret";

    private final JwtProperties props;
    private final Environment environment;
    private SecretKey signingKey;

    @PostConstruct
    void init() {
        final String secret = props.secret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret is required");
        }
        if (secret.startsWith(DEV_SECRET_PREFIX)
                && !environment.acceptsProfiles(Profiles.of("dev", "test"))) {
            throw new IllegalStateException(
                    "JWT_SECRET must be set outside the dev and test profiles");
        }
        // Treat as raw UTF-8 — must be ≥ 32 bytes for HS256.
        final byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret must be at least 32 bytes (256 bits) — "
                            + "current length: " + bytes.length);
        }
        this.signingKey = Keys.hmacShaKeyFor(bytes);
    }

    public IssuedToken issueAccessToken(UUID userId, String email, Role role) {
        return issueAccessToken(userId, email, role, false);
    }

    public IssuedToken issueAccessToken(UUID userId, String email, Role role, boolean mfa) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.accessTokenTtl());
        String token = Jwts.builder()
                .issuer(props.issuer())
                .subject(userId.toString())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_MFA, mfa ? Boolean.TRUE : null)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
        return new IssuedToken(token, exp);
    }

    /** Five-minute proof that the admin's password was right; the code step needs it. */
    public IssuedToken issueAdminChallenge(UUID userId) {
        Instant now = Instant.now();
        Instant exp = now.plus(CHALLENGE_TTL);
        String token = Jwts.builder()
                .issuer(props.issuer() + CHALLENGE_SUFFIX)
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
        return new IssuedToken(token, exp);
    }

    /** @return the admin's id from a valid challenge, or throws {@link UnauthorizedException}. */
    public UUID parseAdminChallenge(String token) {
        try {
            String sub = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(props.issuer() + CHALLENGE_SUFFIX)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return UUID.fromString(sub);
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID);
        }
    }

    /** @return verified claims, or throws {@link UnauthorizedException}. */
    public Claims parse(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(props.issuer())
                    .build()
                    .parseSignedClaims(token);
            return jws.getPayload();
        } catch (ExpiredJwtException e) {
            throw new UnauthorizedException(ErrorCode.AUTH_TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID);
        }
    }

    public AppUserPrincipal toPrincipal(Claims claims) {
        return AppUserPrincipal.builder()
                .id(UUID.fromString(claims.getSubject()))
                .email(claims.get(CLAIM_EMAIL, String.class))
                .role(Role.valueOf(claims.get(CLAIM_ROLE, String.class)))
                .active(true)
                .build();
    }

    public record IssuedToken(String value, Instant expiresAt) {}
}
