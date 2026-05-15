package az.qazan.backend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Every error the API produces is identified by a stable code.
 * Mobile clients switch on the code; the human message is localized.
 */
@Getter
public enum ErrorCode {

    // ── Generic ────────────────────────────────────────────────
    VALIDATION_FAILED("error.validation", HttpStatus.BAD_REQUEST),
    BAD_REQUEST("error.bad_request", HttpStatus.BAD_REQUEST),
    NOT_FOUND("error.not_found", HttpStatus.NOT_FOUND),
    CONFLICT("error.conflict", HttpStatus.CONFLICT),
    UNAUTHORIZED("error.unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("error.forbidden", HttpStatus.FORBIDDEN),
    INTERNAL("error.internal", HttpStatus.INTERNAL_SERVER_ERROR),

    // ── Auth ───────────────────────────────────────────────────
    AUTH_INVALID_CREDENTIALS("error.auth.invalid_credentials", HttpStatus.UNAUTHORIZED),
    AUTH_EMAIL_TAKEN("error.auth.email_taken", HttpStatus.CONFLICT),
    AUTH_TOKEN_EXPIRED("error.auth.token_expired", HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_INVALID("error.auth.token_invalid", HttpStatus.UNAUTHORIZED),
    AUTH_REFRESH_INVALID("error.auth.refresh_invalid", HttpStatus.UNAUTHORIZED),
    AUTH_ACCOUNT_DISABLED("error.auth.account_disabled", HttpStatus.FORBIDDEN),

    // ── User ───────────────────────────────────────────────────
    USER_NOT_FOUND("error.user.not_found", HttpStatus.NOT_FOUND),
    USER_PASSWORD_MISMATCH("error.user.password_mismatch", HttpStatus.BAD_REQUEST);

    private final String messageKey;
    private final HttpStatus status;

    ErrorCode(String messageKey, HttpStatus status) {
        this.messageKey = messageKey;
        this.status = status;
    }
}
