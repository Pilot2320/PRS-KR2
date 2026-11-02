package wallet.service;

import org.junit.jupiter.api.Test;
import wallet.exception.BusinessException;
import wallet.model.Client;
import wallet.model.Transaction;
import wallet.model.TransactionType;
import wallet.model.Wallet;
import wallet.model.WalletStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatisticsServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 2);

    private final StatisticsService service = new StatisticsService();

    @Test
    void calculatesIndicators() {
        List<Client> clients = List.of(
                new Client(1, "Иван Петров", "ivan@mail.ru", "+79000000001", LocalDateTime.of(2026, 1, 1, 10, 0)),
                new Client(2, "Мария Соколова", "maria@mail.ru", "+79000000002", LocalDateTime.of(2026, 2, 1, 10, 0)));
        List<Wallet> wallets = List.of(
                wallet(1, "WLT-000001", "6500.00", WalletStatus.ACTIVE, LocalDate.of(2028, 8, 1)),
                wallet(2, "WLT-000002", "150.00", WalletStatus.ACTIVE, LocalDate.of(2025, 5, 1)),
                wallet(3, "WLT-000003", "0.00", WalletStatus.CLOSED, LocalDate.of(2025, 1, 1)),
                wallet(4, "WLT-000004", "1000.00", WalletStatus.BLOCKED, LocalDate.of(2027, 1, 1)));
        List<Transaction> transactions = List.of(
                transaction(1, TransactionType.TOP_UP, "10000.00", LocalDateTime.of(2026, 9, 1, 9, 0)),
                transaction(1, TransactionType.TRANSFER, "1500.00", LocalDateTime.of(2026, 9, 3, 18, 0)),
                transaction(1, TransactionType.WITHDRAWAL, "2000.00", LocalDateTime.of(2026, 8, 5, 12, 0)));

        Statistics statistics = service.calculate(clients, wallets, transactions,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), TODAY);

        assertThat(statistics.clients()).isEqualTo(2);
        assertThat(statistics.wallets()).isEqualTo(4);
        assertThat(statistics.activeWallets()).isEqualTo(2);
        assertThat(statistics.expiredWallets()).as("закрытый просроченный кошелёк не считается").isEqualTo(1);
        assertThat(statistics.activeShare()).isEqualByComparingTo("50.0");
        assertThat(statistics.totalBalance()).isEqualByComparingTo("7650.00");
        assertThat(statistics.averageBalance()).isEqualByComparingTo("1912.50");
        assertThat(statistics.richestWallet().getWalletNumber()).isEqualTo("WLT-000001");
        assertThat(statistics.topUpSum()).isEqualByComparingTo("10000.00");
        assertThat(statistics.outgoingSum()).isEqualByComparingTo("3500.00");
        assertThat(statistics.averageAmount()).isEqualByComparingTo("4500.00");
        assertThat(statistics.minAmount()).isEqualByComparingTo("1500.00");
        assertThat(statistics.maxAmount()).isEqualByComparingTo("10000.00");
        assertThat(statistics.periodTransactions()).isEqualTo(2);
        assertThat(statistics.periodIncome()).isEqualByComparingTo("10000.00");
        assertThat(statistics.periodOutcome()).isEqualByComparingTo("1500.00");
        assertThat(statistics.indicators()).hasSizeGreaterThanOrEqualTo(5);
    }

    @Test
    void emptyDatabaseGivesZeroesInsteadOfErrors() {
        Statistics statistics = service.calculate(List.of(), List.of(), List.of(), null, null, TODAY);

        assertThat(statistics.wallets()).isZero();
        assertThat(statistics.averageBalance()).isNull();
        assertThat(statistics.activeShare()).isNull();
        assertThat(statistics.totalBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(statistics.periodTitle()).isEqualTo("за всё время");
    }

    @Test
    void rejectsReversedPeriod() {
        assertThatThrownBy(() -> service.calculate(List.of(), List.of(), List.of(),
                LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 1), TODAY))
                .isInstanceOf(BusinessException.class);
    }

    private static Wallet wallet(long id, String number, String balance, WalletStatus status, LocalDate expiry) {
        Wallet wallet = new Wallet();
        wallet.setId(id);
        wallet.setWalletNumber(number);
        wallet.setBalance(new BigDecimal(balance));
        wallet.setStatus(status);
        wallet.setExpiryDate(expiry);
        wallet.setDailyLimit(WalletService.DAILY_LIMIT);
        return wallet;
    }

    private static Transaction transaction(long walletId, TransactionType type, String amount, LocalDateTime at) {
        Transaction transaction = new Transaction();
        transaction.setWalletId(walletId);
        transaction.setType(type);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setCreatedAt(at);
        return transaction;
    }
}
