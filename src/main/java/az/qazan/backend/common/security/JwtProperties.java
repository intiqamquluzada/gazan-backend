package az.qazan.backend.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Bound to the {@code app.jwt.*} section of application.yml.
 * The secret MUST be at least 256 bits (32 bytes) for HS256.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String issuer,
        String secret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl
) {
}
