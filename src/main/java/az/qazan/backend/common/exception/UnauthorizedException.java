package az.qazan.backend.common.exception;

public class UnauthorizedException extends AppException {

    public UnauthorizedException(ErrorCode code, Object... args) {
        super(code, args);
    }
}
