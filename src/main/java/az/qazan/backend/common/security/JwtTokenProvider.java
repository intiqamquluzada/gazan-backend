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

    private final JwtProperties props;
    private SecretKey signingKey;

    @PostConstruct
    void init() {
        final String secret = props.secret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret is required");
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
        Instant now = Instant.now();
        Instant exp = now.plus(props.accessTokenTtl());
        String token = Jwts.builder()
                .issuer(props.issuer())
                .subject(userId.toString())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLE, role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
        return new IssuedToken(token, exp);
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
