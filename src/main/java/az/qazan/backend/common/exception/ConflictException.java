package az.qazan.backend.common.exception;

public class ConflictException extends AppException {

    public ConflictException(ErrorCode code, Object... args) {
        super(code, args);
    }
}
