package wallet.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Ошибки ввода по полям формы: ключ — имя поля, значение — что нужно исправить. */
public class ValidationException extends BusinessException {
    private final Map<String, String> errors;

    public ValidationException(Map<String, String> errors) {
        super(String.join("\n", errors.values()));
        this.errors = Collections.unmodifiableMap(new LinkedHashMap<>(errors));
    }

    public static ValidationException of(String field, String message) {
        return new ValidationException(Map.of(field, message));
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
