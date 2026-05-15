package az.qazan.backend.common.exception;

public class NotFoundException extends AppException {

    public NotFoundException(ErrorCode code, Object... args) {
        super(code, args);
    }

    public static NotFoundException of(ErrorCode code, Object... args) {
        return new NotFoundException(code, args);
    }
}
