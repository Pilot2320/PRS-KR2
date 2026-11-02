package wallet.service.filter;

import org.junit.jupiter.api.Test;
import wallet.dto.TransactionView;
import wallet.exception.BusinessException;
import wallet.model.Client;
import wallet.model.Transaction;
import wallet.model.TransactionType;
import wallet.model.Wallet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionFilterTest {
    private final TransactionView groceries = view(TransactionType.TRANSFER, "Продукты", "Магазин Перекрёсток",
            LocalDateTime.of(2026, 9, 3, 18, 20));

    @Test
    void searchIgnoresCaseAndLetterYo() {
        assertThat(new TransactionFilter("перекресток", null, null, null, null).matches(groceries)).isTrue();
        assertThat(new TransactionFilter("ПЕТРОВ wlt-000001", null, null, null, null).matches(groceries)).isTrue();
        assertThat(new TransactionFilter("Соколова", null, null, null, null).matches(groceries)).isFalse();
    }

    @Test
    void filtersByTypeWalletAndPeriod() {
        assertThat(new TransactionFilter(null, TransactionType.TRANSFER, 1L, null, null).matches(groceries)).isTrue();
        assertThat(new TransactionFilter(null, TransactionType.TOP_UP, null, null, null).matches(groceries)).isFalse();
        assertThat(new TransactionFilter(null, null, 2L, null, null).matches(groceries)).isFalse();
        assertThat(new TransactionFilter(null, null, null, LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 3))
                .matches(groceries)).as("границы периода включаются").isTrue();
        assertThat(new TransactionFilter(null, null, null, LocalDate.of(2026, 9, 4), null).matches(groceries)).isFalse();
    }

    @Test
    void reversedPeriodIsAnError() {
        assertThatThrownBy(() -> new TransactionFilter(null, null, null,
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 1)).validate())
                .isInstanceOf(BusinessException.class);
    }

    private static TransactionView view(TransactionType type, String description, String counterparty, LocalDateTime at) {
        Transaction transaction = new Transaction();
        transaction.setWalletId(1);
        transaction.setType(type);
        transaction.setAmount(new BigDecimal("1500.00"));
        transaction.setDescription(description);
        transaction.setCounterparty(counterparty);
        transaction.setCreatedAt(at);
        Wallet wallet = new Wallet();
        wallet.setId(1);
        wallet.setWalletNumber("WLT-000001");
        Client owner = new Client(1, "Иван Петров", "ivan@mail.ru", "+79000000001", at);
        return new TransactionView(transaction, wallet, owner);
    }
}
