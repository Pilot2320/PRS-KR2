package wallet.exception;

/** Ошибка, сообщение которой можно показать пользователю. */
public class AppException extends RuntimeException {
    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
