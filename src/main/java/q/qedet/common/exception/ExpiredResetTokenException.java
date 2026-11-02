package q.qedet.common.exception;

public class ExpiredResetTokenException extends BadRequestException {

    public ExpiredResetTokenException(String message) {
        super(message);
    }
}
