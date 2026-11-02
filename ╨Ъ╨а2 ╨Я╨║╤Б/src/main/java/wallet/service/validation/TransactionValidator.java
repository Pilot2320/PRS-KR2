package wallet.service.validation;

import wallet.dto.TransactionForm;
import wallet.exception.ValidationException;
import wallet.model.TransactionType;
import wallet.util.Formats;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/** Проверки формы операции, которые не требуют обращения к базе данных. */
public final class TransactionValidator {
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000.00");
    public static final int MAX_DESCRIPTION_LENGTH = 500;

    private static final Pattern AMOUNT = Pattern.compile("\\d{1,13}(\\.\\d{1,2})?");

    private TransactionValidator() {
    }

    public record Checked(BigDecimal amount, String description) {
    }

    public static Checked validate(TransactionForm form) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (form.walletId() == null) {
            errors.put(TransactionForm.WALLET, "Выберите кошелёк, по которому проводится операция");
        }
        if (form.type() == null) {
            errors.put(TransactionForm.TYPE, "Выберите тип операции");
        }
        amountError(form.amount()).ifPresent(message -> errors.put(TransactionForm.AMOUNT, message));

        String description = form.description() == null ? "" : form.description().trim();
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            errors.put(TransactionForm.DESCRIPTION, "Описание не длиннее " + MAX_DESCRIPTION_LENGTH
                    + " символов, сейчас " + description.length());
        }
        if (form.type() == TransactionType.TRANSFER) {
            if (form.targetWalletId() == null) {
                errors.put(TransactionForm.TARGET, "Выберите кошелёк получателя");
            } else if (form.targetWalletId().equals(form.walletId())) {
                errors.put(TransactionForm.TARGET, "Нельзя перевести деньги на тот же кошелёк");
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return new Checked(parseAmount(form.amount()), description);
    }

    public static Optional<String> amountError(String text) {
        String value = normalize(text);
        if (value.isEmpty()) {
            return Optional.of("Введите сумму операции");
        }
        if (value.startsWith("-")) {
            return Optional.of("Сумма должна быть больше нуля");
        }
        if (!AMOUNT.matcher(value).matches()) {
            return Optional.of("Сумма — число, не больше двух знаков после запятой. Пример: 1500,50");
        }
        BigDecimal amount = new BigDecimal(value);
        if (amount.signum() == 0) {
            return Optional.of("Сумма должна быть больше нуля");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            return Optional.of("Сумма одной операции — не больше " + Formats.money(MAX_AMOUNT));
        }
        return Optional.empty();
    }

    /** Разбирает уже проверенную сумму: «1 500,5» → 1500.50. */
    public static BigDecimal parseAmount(String text) {
        return new BigDecimal(normalize(text)).setScale(2);
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().replace(" ", "").replace("\u00A0", "").replace(',', '.');
    }
}
