package wallet.service.validation;

import wallet.dto.ClientForm;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/** Проверка полей клиента. Правила те же, что в КР №1. */
public final class ClientValidator {
    public static final int MAX_NAME_LENGTH = 150;
    public static final int MAX_EMAIL_LENGTH = 150;

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)*\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^\\+7\\d{10}$");
    private static final Pattern NAME_PART = Pattern.compile("\\p{Lu}\\p{Ll}+(?:-\\p{Lu}\\p{Ll}+)*");

    private ClientValidator() {
    }

    /** Убирает лишние пробелы, приводит email к нижнему регистру, телефон — к виду +7XXXXXXXXXX. */
    public static ClientForm normalize(ClientForm form) {
        return new ClientForm(
                trim(form.fullName()),
                trim(form.email()).toLowerCase(Locale.ROOT),
                normalizePhone(form.phone()));
    }

    /** @return ошибки по полям; пустой словарь — данные корректны. */
    public static Map<String, String> validate(ClientForm form) {
        Map<String, String> errors = new LinkedHashMap<>();
        nameError(form.fullName()).ifPresent(message -> errors.put(ClientForm.FULL_NAME, message));
        emailError(form.email()).ifPresent(message -> errors.put(ClientForm.EMAIL, message));
        phoneError(form.phone()).ifPresent(message -> errors.put(ClientForm.PHONE, message));
        return errors;
    }

    static Optional<String> nameError(String name) {
        if (name.isEmpty()) {
            return Optional.of("Введите ФИО клиента");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            return Optional.of("ФИО не длиннее " + MAX_NAME_LENGTH + " символов, сейчас " + name.length());
        }
        if (name.codePoints().anyMatch(symbol -> !Character.isLetter(symbol) && symbol != ' ' && symbol != '-')) {
            return Optional.of("ФИО может содержать только буквы, пробел и дефис");
        }
        if (name.contains("  ") || name.contains("--")) {
            return Optional.of("В ФИО не должно быть двух пробелов или дефисов подряд");
        }
        String[] parts = name.split(" ");
        if (parts.length < 2 || parts.length > 3) {
            return Optional.of("Укажите фамилию и имя через пробел, отчество можно добавить третьим словом");
        }
        for (String part : parts) {
            if (!NAME_PART.matcher(part).matches()) {
                return Optional.of("Каждое слово ФИО начинается с заглавной буквы, дальше строчные, "
                        + "минимум 2 буквы. Пример: Иванов Иван Иванович");
            }
        }
        return Optional.empty();
    }

    static Optional<String> emailError(String email) {
        if (email.isEmpty()) {
            return Optional.of("Введите email");
        }
        if (email.length() > MAX_EMAIL_LENGTH) {
            return Optional.of("Email не длиннее " + MAX_EMAIL_LENGTH + " символов");
        }
        if (!EMAIL.matcher(email).matches()) {
            return Optional.of("Email указан неверно. Пример: ivan.petrov@mail.ru");
        }
        return Optional.empty();
    }

    static Optional<String> phoneError(String phone) {
        if (phone.isEmpty()) {
            return Optional.of("Введите номер телефона");
        }
        if (!PHONE.matcher(phone).matches()) {
            return Optional.of("Телефон — это +7 и 10 цифр, например +79001110001");
        }
        return Optional.empty();
    }

    static String normalizePhone(String phone) {
        String digits = trim(phone).replaceAll("[\\s()\\-]", "");
        if (digits.matches("8\\d{10}")) {
            return "+7" + digits.substring(1);
        }
        if (digits.matches("7\\d{10}")) {
            return "+" + digits;
        }
        return digits;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
