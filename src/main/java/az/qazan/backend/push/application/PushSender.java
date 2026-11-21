package az.qazan.backend.push.application;

import az.qazan.backend.push.domain.DeviceTokenRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Delivers notifications to phones through Firebase Cloud Messaging
 * (HTTP v1 API, authenticated with a service account).
 *
 * <p>Off unless {@code qazan.push.fcm.enabled=true} and a service-account
 * JSON is configured ({@code FCM_CREDENTIALS_FILE} path or
 * {@code FCM_CREDENTIALS_JSON} inline). When off, sends are only logged and
 * the in-app inbox keeps working.
 *
 * <p>Delivery is best-effort and never blocks or fails the caller: it runs
 * after the surrounding transaction commits (so a rolled-back action never
 * pushes), on a small background pool. Tokens Firebase reports as no
 * longer registered are deleted.
 */
@Component
@Slf4j
public class PushSender {

    private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    /** Android channel the app creates on start; keep in sync with the app. */
    static final String ANDROID_CHANNEL = "qazan_default";

    private final boolean enabled;
    private final String credentialsFile;
    private final String credentialsJson;
    private final DeviceTokenRepository tokens;
    private final TransactionTemplate tx;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(
            1, 2, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(500),
            r -> {
                Thread t = new Thread(r, "fcm-sender");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.DiscardOldestPolicy());

    private volatile GoogleCredentials credentials;
    private volatile String projectId;

    public PushSender(@Value("${qazan.push.fcm.enabled:false}") boolean enabled,
                      @Value("${qazan.push.fcm.credentials-file:}") String credentialsFile,
                      @Value("${qazan.push.fcm.credentials-json:}") String credentialsJson,
                      DeviceTokenRepository tokens,
                      PlatformTransactionManager txManager,
                      ObjectMapper json) {
        this.enabled = enabled;
        this.credentialsFile = credentialsFile;
        this.credentialsJson = credentialsJson;
        this.tokens = tokens;
        this.tx = new TransactionTemplate(txManager);
        this.json = json;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void send(List<String> targets, String title, String body) {
        send(targets, title, body, Map.of());
    }

    /**
     * @param data extra key/values for the app, e.g. {@code route} to open
     *             when the notification is tapped.
     */
    public void send(List<String> targets, String title, String body,
                     Map<String, String> data) {
        if (targets == null || targets.isEmpty()) {
            return;
        }
        if (!enabled) {
            log.info("[push] disabled — would deliver \"{}\" to {} device(s)",
                    title, targets.size());
            return;
        }
        List<String> copy = List.copyOf(targets);
        Map<String, String> extra = data == null ? Map.of() : Map.copyOf(data);
        Runnable job = () -> pool.execute(() -> deliver(copy, title, body, extra));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            job.run();
                        }
                    });
        } else {
            job.run();
        }
    }

    private void deliver(List<String> targets, String title, String body,
                         Map<String, String> data) {
        String accessToken;
        try {
            accessToken = accessToken();
        } catch (Exception e) {
            log.warn("[push] FCM credentials unusable: {}", e.getMessage());
            return;
        }
        List<String> dead = new ArrayList<>();
        int ok = 0;
        for (String token : targets) {
            try {
                HttpResponse<String> res = http.send(HttpRequest.newBuilder()
                                .uri(URI.create("https://fcm.googleapis.com/v1/projects/"
                                        + projectId + "/messages:send"))
                                .timeout(Duration.ofSeconds(15))
                                .header("Authorization", "Bearer " + accessToken)
                                .header("Content-Type", "application/json; charset=UTF-8")
                                .POST(HttpRequest.BodyPublishers.ofString(
                                        message(token, title, body, data),
                                        StandardCharsets.UTF_8))
                                .build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (res.statusCode() == 200) {
                    ok++;
                } else if (isDeadToken(res)) {
                    dead.add(token);
                } else {
                    log.warn("[push] FCM {} for one device: {}", res.statusCode(),
                            abbreviate(res.body()));
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("[push] FCM request failed: {}", e.getMessage());
            }
        }
        if (!dead.isEmpty()) {
            try {
                tx.executeWithoutResult(s -> dead.forEach(tokens::deleteByToken));
            } catch (Exception e) {
                log.warn("[push] could not drop stale tokens: {}", e.getMessage());
            }
        }
        log.info("[push] \"{}\" delivered to {}/{} device(s), {} stale removed",
                title, ok, targets.size(), dead.size());
    }

    private String message(String token, String title, String body,
                           Map<String, String> data) throws Exception {
        Map<String, Object> notification = Map.of("title", title, "body", body);
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("token", token);
        msg.put("notification", notification);
        if (!data.isEmpty()) {
            msg.put("data", data);
        }
        msg.put("android", Map.of(
                "priority", "HIGH",
                "notification", Map.of("channel_id", ANDROID_CHANNEL,
                        "default_sound", true)));
        msg.put("apns", Map.of("payload", Map.of("aps", Map.of("sound", "default"))));
        return json.writeValueAsString(Map.of("message", msg));
    }

    /** UNREGISTERED (404) or a malformed token (400 INVALID_ARGUMENT on token). */
    private boolean isDeadToken(HttpResponse<String> res) {
        if (res.statusCode() != 404 && res.statusCode() != 400) {
            return false;
        }
        try {
            JsonNode err = json.readTree(res.body()).path("error");
            String status = err.path("status").asText("");
            if ("NOT_FOUND".equals(status)) {
                return true;
            }
            for (JsonNode d : err.path("details")) {
                if ("UNREGISTERED".equals(d.path("errorCode").asText())) {
                    return true;
                }
            }
            return "INVALID_ARGUMENT".equals(status)
                    && err.path("message").asText("").toLowerCase().contains("registration token");
        } catch (Exception e) {
            return false;
        }
    }

    private synchronized String accessToken() throws Exception {
        if (credentials == null) {
            byte[] raw;
            if (credentialsJson != null && !credentialsJson.isBlank()) {
                raw = credentialsJson.getBytes(StandardCharsets.UTF_8);
            } else if (credentialsFile != null && !credentialsFile.isBlank()) {
                try (InputStream in = new FileInputStream(credentialsFile)) {
                    raw = in.readAllBytes();
                }
            } else {
                throw new IllegalStateException(
                        "set FCM_CREDENTIALS_FILE or FCM_CREDENTIALS_JSON");
            }
            GoogleCredentials c = GoogleCredentials
                    .fromStream(new ByteArrayInputStream(raw)).createScoped(SCOPE);
            String pid = c instanceof ServiceAccountCredentials sa ? sa.getProjectId() : null;
            if (pid == null || pid.isBlank()) {
                pid = json.readTree(raw).path("project_id").asText(null);
            }
            if (pid == null || pid.isBlank()) {
                throw new IllegalStateException("service account has no project_id");
            }
            credentials = c;
            projectId = pid;
        }
        credentials.refreshIfExpired();
        return credentials.getAccessToken().getTokenValue();
    }

    private static String abbreviate(String s) {
        if (s == null) return "";
        String one = s.replaceAll("\\s+", " ");
        return one.length() > 300 ? one.substring(0, 300) + "…" : one;
    }

    @PreDestroy
    void shutdown() {
        pool.shutdown();
    }
}
