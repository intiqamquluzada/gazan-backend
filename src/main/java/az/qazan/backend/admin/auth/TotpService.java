package az.qazan.backend.admin.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.OptionalLong;

/**
 * RFC 6238 one-time codes (30-second steps, 6 digits, HMAC-SHA1) as used
 * by Google Authenticator, 1Password, Authy and friends. Secrets are kept
 * AES-GCM encrypted at rest.
 */
@Service
public class TotpService {

    private static final int STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    /** Accept the previous and next step too, for clock drift. */
    private static final int DRIFT_STEPS = 1;
    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom RNG = new SecureRandom();

    private final SecretKeySpec key;

    public TotpService(@Value("${qazan.admin.totp-key:}") String totpKey,
                       @Value("${app.jwt.secret}") String jwtSecret) {
        String material = totpKey == null || totpKey.isBlank() ? jwtSecret : totpKey;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(("qazan-totp:" + material).getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(digest, "AES");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A fresh 160-bit secret, base32 encoded as authenticator apps expect. */
    public String newSecret() {
        byte[] bytes = new byte[20];
        RNG.nextBytes(bytes);
        return base32(bytes);
    }

    public String otpauthUrl(String issuer, String account, String secret) {
        String label = enc(issuer) + ":" + enc(account);
        return "otpauth://totp/" + label + "?secret=" + secret
                + "&issuer=" + enc(issuer) + "&algorithm=SHA1&digits=" + DIGITS
                + "&period=" + STEP_SECONDS;
    }

    /**
     * @return the matched step when {@code code} is valid now and newer
     *         than {@code lastStep}; empty otherwise.
     */
    public OptionalLong verify(String secret, String code, Long lastStep) {
        if (secret == null || code == null) return OptionalLong.empty();
        String digits = code.replaceAll("\\s", "");
        if (!digits.matches("\\d{" + DIGITS + "}")) return OptionalLong.empty();
        byte[] keyBytes = unbase32(secret);
        long now = Instant.now().getEpochSecond() / STEP_SECONDS;
        for (long step = now - DRIFT_STEPS; step <= now + DRIFT_STEPS; step++) {
            if (lastStep != null && step <= lastStep) continue;
            if (MessageDigest.isEqual(
                    codeAt(keyBytes, step).getBytes(StandardCharsets.US_ASCII),
                    digits.getBytes(StandardCharsets.US_ASCII))) {
                return OptionalLong.of(step);
            }
        }
        return OptionalLong.empty();
    }

    /** Code for the current step; used by tests. */
    public String currentCode(String secret) {
        return codeAt(unbase32(secret), Instant.now().getEpochSecond() / STEP_SECONDS);
    }

    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[12];
            RNG.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] out = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + out.length).put(iv).put(out).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public String decrypt(String stored) {
        try {
            byte[] all = Base64.getDecoder().decode(stored);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, all, 0, 12));
            return new String(c.doFinal(all, 12, all.length - 12), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            // Key changed or value tampered with: treat as not enrolled.
            return null;
        }
    }

    private static String codeAt(byte[] key, long step) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] h = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());
            int off = h[h.length - 1] & 0x0f;
            int bin = ((h[off] & 0x7f) << 24) | ((h[off + 1] & 0xff) << 16)
                    | ((h[off + 2] & 0xff) << 8) | (h[off + 3] & 0xff);
            return String.format("%0" + DIGITS + "d", bin % 1_000_000);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String base32(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0, bits = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff);
            bits += 8;
            while (bits >= 5) {
                sb.append(BASE32.charAt((buffer >> (bits - 5)) & 31));
                bits -= 5;
            }
        }
        if (bits > 0) sb.append(BASE32.charAt((buffer << (5 - bits)) & 31));
        return sb.toString();
    }

    private static byte[] unbase32(String s) {
        String clean = s.replace("=", "").replace(" ", "").toUpperCase();
        ByteBuffer out = ByteBuffer.allocate(clean.length() * 5 / 8);
        int buffer = 0, bits = 0;
        for (char ch : clean.toCharArray()) {
            int v = BASE32.indexOf(ch);
            if (v < 0) continue;
            buffer = (buffer << 5) | v;
            bits += 5;
            if (bits >= 8) {
                out.put((byte) ((buffer >> (bits - 8)) & 0xff));
                bits -= 8;
            }
        }
        return java.util.Arrays.copyOf(out.array(), out.position());
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
