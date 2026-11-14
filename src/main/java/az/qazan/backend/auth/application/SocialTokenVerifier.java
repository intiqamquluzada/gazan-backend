package az.qazan.backend.auth.application;

import az.qazan.backend.common.exception.AppException;
import az.qazan.backend.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Verifies Google and Apple ID tokens against the provider's published
 * keys: signature, expiry, issuer and audience (our client ids).
 */
@Slf4j
@Component
public class SocialTokenVerifier {

    private record Provider(String jwksUrl, Set<String> issuers, Set<String> audiences) {}

    private record CachedKeys(Instant fetchedAt, Map<String, Key> keys) {}

    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final Duration MIN_REFETCH_GAP = Duration.ofMinutes(1);

    private final Map<String, CachedKeys> cache = new ConcurrentHashMap<>();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper;
    private final Provider google;
    private final Provider apple;

    public SocialTokenVerifier(
            ObjectMapper objectMapper,
            @Value("${qazan.social.google-client-ids:}") String googleClientIds,
            @Value("${qazan.social.apple-client-ids:}") String appleClientIds) {
        this.objectMapper = objectMapper;
        this.google = new Provider(
                "https://www.googleapis.com/oauth2/v3/certs",
                Set.of("https://accounts.google.com", "accounts.google.com"),
                parseIds(googleClientIds));
        this.apple = new Provider(
                "https://appleid.apple.com/auth/keys",
                Set.of("https://appleid.apple.com"),
                parseIds(appleClientIds));
    }

    public SocialIdentity verify(String providerName, String idToken) {
        Provider provider = "APPLE".equals(providerName) ? apple : google;
        if (provider.audiences().isEmpty()) {
            throw new AppException(ErrorCode.AUTH_SOCIAL_NOT_CONFIGURED);
        }
        try {
            Claims claims = Jwts.parser()
                    .keyLocator(header -> keyFor(provider, (String) header.get("kid")))
                    .clockSkewSeconds(60)
                    .build()
                    .parseSignedClaims(idToken)
                    .getPayload();
            Set<String> audience = claims.getAudience();
            boolean audienceOk = audience != null
                    && audience.stream().anyMatch(provider.audiences()::contains);
            String subject = claims.getSubject();
            if (!provider.issuers().contains(claims.getIssuer())
                    || !audienceOk || subject == null || subject.isBlank()) {
                throw new AppException(ErrorCode.AUTH_SOCIAL_INVALID);
            }
            Object verified = claims.get("email_verified");
            return new SocialIdentity(
                    subject,
                    claims.get("email", String.class),
                    Boolean.TRUE.equals(verified) || "true".equals(verified),
                    claims.get("name", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Rejected {} ID token: {}", providerName, e.getMessage());
            throw new AppException(ErrorCode.AUTH_SOCIAL_INVALID);
        }
    }

    private Key keyFor(Provider provider, String keyId) {
        if (keyId == null) return null;
        CachedKeys cached = cache.get(provider.jwksUrl());
        Instant now = Instant.now();
        boolean stale = cached == null
                || cached.fetchedAt().plus(CACHE_TTL).isBefore(now);
        boolean rotated = cached != null && !cached.keys().containsKey(keyId)
                && cached.fetchedAt().plus(MIN_REFETCH_GAP).isBefore(now);
        if (stale || rotated) {
            cached = fetch(provider);
            cache.put(provider.jwksUrl(), cached);
        }
        return cached.keys().get(keyId);
    }

    private CachedKeys fetch(Provider provider) {
        try {
            HttpResponse<String> response = http.send(
                    HttpRequest.newBuilder(URI.create(provider.jwksUrl()))
                            .timeout(Duration.ofSeconds(5))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("JWKS status " + response.statusCode());
            }
            Map<String, Key> keys = new HashMap<>();
            KeyFactory factory = KeyFactory.getInstance("RSA");
            for (JsonNode jwk : objectMapper.readTree(response.body()).path("keys")) {
                if (!"RSA".equals(jwk.path("kty").asText())) continue;
                keys.put(jwk.path("kid").asText(), factory.generatePublic(
                        new RSAPublicKeySpec(
                                unsigned(jwk.path("n").asText()),
                                unsigned(jwk.path("e").asText()))));
            }
            return new CachedKeys(Instant.now(), keys);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.AUTH_SOCIAL_INVALID);
        } catch (Exception e) {
            log.warn("Could not load signing keys from {}: {}",
                    provider.jwksUrl(), e.getMessage());
            throw new AppException(ErrorCode.AUTH_SOCIAL_INVALID);
        }
    }

    private static BigInteger unsigned(String base64Url) {
        return new BigInteger(1, Base64.getUrlDecoder().decode(base64Url));
    }

    private static Set<String> parseIds(String csv) {
        if (csv == null || csv.isBlank()) return Collections.emptySet();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
