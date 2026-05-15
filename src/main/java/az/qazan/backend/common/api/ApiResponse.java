package az.qazan.backend.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Generic envelope for non-paginated successful responses.
 * Most endpoints return their domain DTO directly; this is provided for
 * actions like "logout" that have no natural body.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        T data,
        String message,
        Instant timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, null, Instant.now());
    }

    public static ApiResponse<Void> message(String message) {
        return new ApiResponse<>(null, message, Instant.now());
    }
}
