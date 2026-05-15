package az.qazan.backend.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * RFC 7807-inspired error payload returned by the global handler.
 *
 * @param code      Stable machine-readable error code
 * @param message   Localized human message
 * @param status    HTTP status code
 * @param path      Request path that produced the error
 * @param timestamp When the error occurred (server time)
 * @param fields    Per-field validation errors, when applicable
 * @param details   Free-form extra context (rare)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String code,
        String message,
        int status,
        String path,
        Instant timestamp,
        List<FieldError> fields,
        Map<String, String> details
) {

    public record FieldError(String field, String message, Object rejectedValue) {}
}
