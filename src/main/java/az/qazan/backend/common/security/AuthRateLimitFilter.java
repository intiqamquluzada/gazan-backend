package az.qazan.backend.common.security;

import az.qazan.backend.common.exception.ApiError;
import az.qazan.backend.common.exception.ErrorCode;
import az.qazan.backend.common.i18n.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed one-minute window per client IP and endpoint on the public auth
 * routes. In memory on purpose: a single backend instance runs today, and
 * losing the counters on restart is harmless.
 */
@Component
@Order(-150)
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000;
    private static final int MAX_TRACKED = 20_000;

    private static final Set<String> LIMITED = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/phone/start",
            "/api/v1/auth/social",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/email/start",
            "/api/v1/auth/email/verify");

    /** Routes that send mail or guess a code get a quarter of the budget. */
    private static final Set<String> STRICT = Set.of(
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/email/start",
            "/api/v1/auth/email/verify");

    private record Window(long startedAt, int count) {}

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final MessageService messages;
    private final int perMinute;

    public AuthRateLimitFilter(ObjectMapper objectMapper,
                               MessageService messages,
                               @Value("${qazan.rate-limit.per-minute:20}") int perMinute) {
        this.objectMapper = objectMapper;
        this.messages = messages;
        this.perMinute = perMinute;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest req) {
        return !"POST".equalsIgnoreCase(req.getMethod())
                || !LIMITED.contains(req.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest req,
            @NonNull HttpServletResponse res,
            @NonNull FilterChain chain) throws ServletException, IOException {
        String path = req.getRequestURI();
        int limit = STRICT.contains(path) ? Math.max(1, perMinute / 4) : perMinute;
        if (!allow(path + "|" + clientIp(req), limit)) {
            reject(res, path);
            return;
        }
        chain.doFilter(req, res);
    }

    private boolean allow(String key, int limit) {
        long now = System.currentTimeMillis();
        if (windows.size() > MAX_TRACKED) {
            windows.entrySet().removeIf(
                    e -> now - e.getValue().startedAt() >= WINDOW_MILLIS);
        }
        Window w = windows.merge(key, new Window(now, 1), (old, fresh) ->
                now - old.startedAt() >= WINDOW_MILLIS
                        ? fresh
                        : new Window(old.startedAt(), old.count() + 1));
        return w.count() <= limit;
    }

    /**
     * The address to rate-limit by. X-Forwarded-For is client-controlled,
     * so it is trusted only when the request comes from our own reverse
     * proxy (Apache on the same host); then the RIGHTMOST entry — the one
     * the proxy appended — is the real client. Otherwise the socket address.
     */
    static String clientIp(HttpServletRequest req) {
        String remote = req.getRemoteAddr();
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank() && isTrustedProxy(remote)) {
            String[] hops = forwarded.split(",");
            String last = hops[hops.length - 1].trim();
            if (!last.isEmpty()) {
                return last;
            }
        }
        return remote;
    }

    private static boolean isTrustedProxy(String addr) {
        return addr != null && (addr.equals("127.0.0.1")
                || addr.equals("::1")
                || addr.equals("0:0:0:0:0:0:0:1"));
    }

    private void reject(HttpServletResponse res, String path) throws IOException {
        ErrorCode code = ErrorCode.RATE_LIMITED;
        res.setStatus(code.getStatus().value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(res.getWriter(), new ApiError(
                code.name(), messages.get(code), code.getStatus().value(),
                path, Instant.now(), null, null));
    }
}
