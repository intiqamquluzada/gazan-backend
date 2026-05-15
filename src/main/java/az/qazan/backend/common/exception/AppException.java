package az.qazan.backend.common.exception;

import lombok.Getter;

import java.util.Map;

/**
 * Root of all domain exceptions. Carries an {@link ErrorCode} and an
 * optional set of message arguments used by the i18n MessageSource.
 */
@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] messageArgs;
    private final Map<String, String> details;

    public AppException(ErrorCode errorCode) {
        this(errorCode, null, new Object[0]);
    }

    public AppException(ErrorCode errorCode, Object... args) {
        this(errorCode, null, args);
    }

    public AppException(ErrorCode errorCode, Map<String, String> details, Object... args) {
        super(errorCode.getMessageKey());
        this.errorCode = errorCode;
        this.messageArgs = args == null ? new Object[0] : args;
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }
}
