package az.qazan.backend.common.exception;

import az.qazan.backend.common.i18n.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

/**
 * Single source of truth for HTTP error responses. Every exception
 * surfaces as an {@link ApiError} with a localized message.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageService messages;

    // ───────────────────────── domain ─────────────────────────

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiError> handleAppException(
            AppException ex, HttpServletRequest req) {
        ErrorCode code = ex.getErrorCode();
        return build(code, messages.get(code, ex.getMessageArgs()), req,
                null, ex.getDetails().isEmpty() ? null : ex.getDetails());
    }

    // ─────────────────────── validation ───────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ApiError.FieldError> fields = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(this::toField)
                .toList();
        return build(ErrorCode.VALIDATION_FAILED,
                messages.get(ErrorCode.VALIDATION_FAILED), req, fields, null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(
            ConstraintViolationException ex, HttpServletRequest req) {
        List<ApiError.FieldError> fields = ex.getConstraintViolations().stream()
                .map(this::toField)
                .toList();
        return build(ErrorCode.VALIDATION_FAILED,
                messages.get(ErrorCode.VALIDATION_FAILED), req, fields, null);
    }

    // ───────────────────── request shape ──────────────────────

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpRequestMethodNotSupportedException.class,
    })
    public ResponseEntity<ApiError> handleBadRequest(
            Exception ex, HttpServletRequest req) {
        log.debug("Bad request", ex);
        return build(ErrorCode.BAD_REQUEST,
                messages.get(ErrorCode.BAD_REQUEST), req, null, null);
    }

    // ──────────────────────── security ────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest req) {
        return build(ErrorCode.AUTH_INVALID_CREDENTIALS,
                messages.get(ErrorCode.AUTH_INVALID_CREDENTIALS), req, null, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(
            AuthenticationException ex, HttpServletRequest req) {
        return build(ErrorCode.UNAUTHORIZED,
                messages.get(ErrorCode.UNAUTHORIZED), req, null, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleForbidden(
            AccessDeniedException ex, HttpServletRequest req) {
        return build(ErrorCode.FORBIDDEN,
                messages.get(ErrorCode.FORBIDDEN), req, null, null);
    }

    // ────────────────────── catch-all ─────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAny(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception at {}", req.getRequestURI(), ex);
        return build(ErrorCode.INTERNAL,
                messages.get(ErrorCode.INTERNAL), req, null, null);
    }

    // ───────────────────────── helpers ────────────────────────

    private ResponseEntity<ApiError> build(
            ErrorCode code,
            String message,
            HttpServletRequest req,
            List<ApiError.FieldError> fields,
            java.util.Map<String, String> details) {
        ApiError body = new ApiError(
                code.name(),
                message,
                code.getStatus().value(),
                req.getRequestURI(),
                Instant.now(),
                fields,
                details
        );
        return ResponseEntity.status(code.getStatus()).body(body);
    }

    private ApiError.FieldError toField(FieldError fe) {
        return new ApiError.FieldError(
                fe.getField(),
                fe.getDefaultMessage(),
                fe.getRejectedValue()
        );
    }

    private ApiError.FieldError toField(ConstraintViolation<?> cv) {
        return new ApiError.FieldError(
                cv.getPropertyPath().toString(),
                cv.getMessage(),
                cv.getInvalidValue()
        );
    }
}
