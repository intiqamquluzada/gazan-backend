package az.qazan.backend.common.exception;

public class BadRequestException extends AppException {

    public BadRequestException(ErrorCode code, Object... args) {
        super(code, args);
    }
}
