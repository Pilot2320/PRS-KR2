package wallet.service.validation;

import org.junit.jupiter.api.Test;
import wallet.dto.TransactionForm;
import wallet.exception.ValidationException;
import wallet.model.TransactionType;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionValidatorTest {

    @Test
    void parsesAmountWithCommaAndSpaces() {
        TransactionValidator.Checked checked = TransactionValidator.validate(
                new TransactionForm(1L, TransactionType.TOP_UP, " 1 500,5 ", "  Зарплата ", null));

        assertThat(checked.amount()).isEqualByComparingTo(new BigDecimal("1500.50"));
        assertThat(checked.amount().scale()).isEqualTo(2);
        assertThat(checked.description()).isEqualTo("Зарплата");
    }

    @Test
    void rejectsWrongAmounts() {
        assertThat(TransactionValidator.amountError("")).isPresent();
        assertThat(TransactionValidator.amountError("0")).isPresent();
        assertThat(TransactionValidator.amountError("-10")).isPresent();
        assertThat(TransactionValidator.amountError("10.555")).isPresent();
        assertThat(TransactionValidator.amountError("сто")).isPresent();
        assertThat(TransactionValidator.amountError("1000000.01")).isPresent();
        assertThat(TransactionValidator.amountError("1000000")).isEmpty();
    }

    @Test
    void transferNeedsAnotherWallet() {
        assertThatThrownBy(() -> TransactionValidator.validate(
                new TransactionForm(3L, TransactionType.TRANSFER, "100", "", 3L)))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.getErrors()).containsOnlyKeys(TransactionForm.TARGET));

        assertThatThrownBy(() -> TransactionValidator.validate(
                new TransactionForm(3L, TransactionType.TRANSFER, "100", "", null)))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.getErrors()).containsOnlyKeys(TransactionForm.TARGET));
    }

    @Test
    void collectsAllFieldErrors() {
        assertThatThrownBy(() -> TransactionValidator.validate(
                new TransactionForm(null, null, "abc", "x".repeat(501), null)))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.getErrors()).containsOnlyKeys(
                                TransactionForm.WALLET, TransactionForm.TYPE,
                                TransactionForm.AMOUNT, TransactionForm.DESCRIPTION));
    }
}
