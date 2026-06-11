package az.qazan.backend.push.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Delivers a notification to physical devices via Firebase Cloud Messaging.
 *
 * <p>Gated behind {@code qazan.push.fcm.enabled} (default {@code false}) so
 * the app runs fully without a Firebase project — every other piece of the
 * push pipeline (token registration, the in-app inbox) works regardless.
 * When disabled, sends are logged and dropped.
 *
 * <p>To go live, provision a Firebase service account and implement
 * {@link #deliverViaFcm} — see {@code gazan-mobile/PUSH_SETUP.md}. Delivery
 * is best-effort: a failure here must never break notification creation,
 * so {@link #send} swallows all errors.
 */
@Component
@Slf4j
public class PushSender {

    @Value("${qazan.push.fcm.enabled:false}")
    private boolean enabled;

    public void send(List<String> tokens, String title, String body) {
        if (tokens == null || tokens.isEmpty()) {
            return;
        }
        if (!enabled) {
            log.info("[push] disabled — would deliver \"{}\" to {} device(s)",
                    title, tokens.size());
            return;
        }
        try {
            deliverViaFcm(tokens, title, body);
        } catch (Exception e) {
            log.warn("[push] FCM delivery failed for {} device(s): {}",
                    tokens.size(), e.getMessage());
        }
    }

    /**
     * Real FCM HTTP v1 delivery. Implement once a service account exists:
     * <ol>
     *   <li>Add {@code com.google.auth:google-auth-library-oauth2-http}.</li>
     *   <li>Load the service-account JSON (path in {@code qazan.push.fcm.credentials}).</li>
     *   <li>Mint an OAuth2 access token (scope {@code .../firebase.messaging}).</li>
     *   <li>POST {@code https://fcm.googleapis.com/v1/projects/<id>/messages:send}
     *       with {@code {message:{token, notification:{title, body}}}} per token;
     *       drop tokens that come back {@code UNREGISTERED}.</li>
     * </ol>
     */
    private void deliverViaFcm(List<String> tokens, String title, String body) {
        throw new UnsupportedOperationException("FCM sender not yet configured");
    }
}
