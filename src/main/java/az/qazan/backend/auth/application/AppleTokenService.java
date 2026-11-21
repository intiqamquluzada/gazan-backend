package az.qazan.backend.auth.application;

import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

/**
 * Sign in with Apple server-to-server calls.
 *
 * <p>App Store guideline 5.1.1(v): when an account that signed in with
 * Apple is deleted, the app must revoke the user's Apple tokens. On
 * sign-in we trade the one-time authorization code for a refresh token
 * (stored on the user); on deletion we revoke it.
 *
 * <p>Needs the Sign in with Apple key (.p8) from the developer account:
 * {@code APPLE_TEAM_ID}, {@code APPLE_KEY_ID}, {@code APPLE_PRIVATE_KEY_FILE}.
 * Without them the feature is simply off (sign-in itself still works —
 * identity tokens are verified separately with Apple's public keys).
 */
@Service
@Slf4j
public class AppleTokenService {

    private static final String APPLE = "https://appleid.apple.com";

    private final String teamId;
    private final String keyId;
    private final String keyFile;
    private final String clientId;
    private final RestClient http = RestClient.create();
    private volatile PrivateKey key;

    public AppleTokenService(
            @Value("${qazan.social.apple.team-id:}") String teamId,
            @Value("${qazan.social.apple.key-id:}") String keyId,
            @Value("${qazan.social.apple.private-key-file:}") String keyFile,
            @Value("${qazan.social.apple-client-ids:}") String clientIds) {
        this.teamId = teamId == null ? "" : teamId.trim();
        this.keyId = keyId == null ? "" : keyId.trim();
        this.keyFile = keyFile == null ? "" : keyFile.trim();
        String first = clientIds == null ? "" : clientIds.split(",")[0].trim();
        this.clientId = first;
    }

    public boolean enabled() {
        return !teamId.isEmpty() && !keyId.isEmpty() && !keyFile.isEmpty()
                && !clientId.isEmpty();
    }

    /** Authorization code → refresh token, or null when it cannot be done. */
    public String exchangeCode(String authorizationCode) {
        if (!enabled() || authorizationCode == null || authorizationCode.isBlank()) {
            return null;
        }
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret());
            form.add("code", authorizationCode);
            form.add("grant_type", "authorization_code");
            @SuppressWarnings("unchecked")
            Map<String, Object> res = http.post().uri(APPLE + "/auth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
            Object rt = res == null ? null : res.get("refresh_token");
            return rt instanceof String s && !s.isBlank() ? s : null;
        } catch (Exception e) {
            log.warn("[apple] code exchange failed: {}", e.getMessage());
            return null;
        }
    }

    /** Revokes a refresh token; best effort, never throws. */
    public void revoke(String refreshToken) {
        if (!enabled() || refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret());
            form.add("token", refreshToken);
            form.add("token_type_hint", "refresh_token");
            http.post().uri(APPLE + "/auth/revoke")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[apple] token revoked");
        } catch (Exception e) {
            log.warn("[apple] revoke failed: {}", e.getMessage());
        }
    }

    /** Short-lived ES256 client secret, as Apple requires. */
    private String clientSecret() throws Exception {
        Instant now = Instant.now();
        return Jwts.builder()
                .header().keyId(keyId).and()
                .issuer(teamId)
                .subject(clientId)
                .audience().add(APPLE).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .signWith(privateKey(), Jwts.SIG.ES256)
                .compact();
    }

    private PrivateKey privateKey() throws Exception {
        PrivateKey k = key;
        if (k == null) {
            String pem = Files.readString(Path.of(keyFile), StandardCharsets.UTF_8)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            k = KeyFactory.getInstance("EC")
                    .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
            key = k;
        }
        return k;
    }
}
