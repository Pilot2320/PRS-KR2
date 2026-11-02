package wallet.exception;

/** Нарушено бизнес-правило: например, нельзя закрыть кошелёк с деньгами. */
public class BusinessException extends AppException {
    public BusinessException(String message) {
        super(message);
    }
}
