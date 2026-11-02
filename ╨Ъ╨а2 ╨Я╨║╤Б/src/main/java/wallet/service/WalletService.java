package wallet.service;

import wallet.db.Database;
import wallet.dto.WalletView;
import wallet.exception.BusinessException;
import wallet.exception.ValidationException;
import wallet.model.Client;
import wallet.model.Currency;
import wallet.model.PaymentSystem;
import wallet.model.Wallet;
import wallet.model.WalletStatus;
import wallet.repository.ClientRepository;
import wallet.repository.TransactionRepository;
import wallet.repository.WalletRepository;
import wallet.service.validation.CardNumbers;
import wallet.util.Formats;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WalletService {
    public static final String STATUS_FIELD = "status";
    public static final BigDecimal DAILY_LIMIT = new BigDecimal("100000.00");
    public static final int VALIDITY_YEARS = 3;
    private static final int CARD_NUMBER_ATTEMPTS = 30;

    private final Database database;
    private final WalletRepository walletRepository;
    private final ClientRepository clientRepository;
    private final TransactionRepository transactionRepository;

    public WalletService(Database database, WalletRepository walletRepository,
                         ClientRepository clientRepository, TransactionRepository transactionRepository) {
        this.database = database;
        this.walletRepository = walletRepository;
        this.clientRepository = clientRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<WalletView> findAll() {
        Map<Long, Client> owners = clientRepository.findAll().stream()
                .collect(Collectors.toMap(Client::getId, Function.identity()));
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        Map<Long, BigDecimal> spentToday = transactionRepository.sumTransfersByWallet(dayStart, dayStart.plusDays(1));
        return walletRepository.findAll().stream()
                .map(wallet -> new WalletView(wallet, owners.get(wallet.getClientId()),
                        limitLeft(wallet, spentToday.getOrDefault(wallet.getId(), BigDecimal.ZERO))))
                .toList();
    }

    /** Остаток дневного лимита: потолок 100 000 минус переводы за сегодня, но не меньше нуля. */
    public static BigDecimal limitLeft(Wallet wallet, BigDecimal spentToday) {
        BigDecimal left = wallet.getDailyLimit().subtract(spentToday);
        return left.signum() < 0 ? BigDecimal.ZERO.setScale(2) : left;
    }

    /** Выпуск кошелька новому клиенту. Вызывается внутри транзакции, в которой сохраняется клиент. */
    Wallet issueFor(Connection connection, Client client) throws SQLException {
        Wallet wallet = new Wallet();
        wallet.setClientId(client.getId());
        wallet.setWalletNumber(nextWalletNumber(connection));
        wallet.setCardNumber(nextCardNumber(connection));
        wallet.setHolderName(client.getFullName().toUpperCase(Locale.ROOT));
        wallet.setPaymentSystem(PaymentSystem.MIR);
        wallet.setExpiryDate(YearMonth.now().plusYears(VALIDITY_YEARS).atDay(1));
        wallet.setCurrency(Currency.RUB);
        wallet.setBalance(BigDecimal.ZERO.setScale(2));
        wallet.setStatus(WalletStatus.ACTIVE);
        wallet.setDailyLimit(DAILY_LIMIT);
        wallet.setCreatedAt(LocalDateTime.now());
        return walletRepository.insert(connection, wallet);
    }

    public Wallet changeStatus(long walletId, WalletStatus next) {
        if (next == null) {
            throw ValidationException.of(STATUS_FIELD, "Выберите новый статус кошелька");
        }
        return database.inTransaction(connection -> {
            Wallet wallet = walletRepository.findByIdForUpdate(connection, walletId)
                    .orElseThrow(() -> new BusinessException("Кошелёк не найден: возможно, его удалили. Обновите данные"));
            WalletStatus current = wallet.getStatus();
            if (current == next) {
                throw ValidationException.of(STATUS_FIELD, "Кошелёк уже в статусе «" + next.getTitle() + "»");
            }
            if (!current.canTransitTo(next)) {
                throw ValidationException.of(STATUS_FIELD, "Нельзя перевести кошелёк из статуса «"
                        + current.getTitle() + "» в «" + next.getTitle() + "»");
            }
            if (next == WalletStatus.CLOSED && wallet.getBalance().signum() != 0) {
                throw ValidationException.of(STATUS_FIELD, "Закрыть можно только кошелёк с нулевым балансом. Сейчас на нём "
                        + Formats.money(wallet.getBalance()) + " — сначала выведите средства");
            }
            walletRepository.updateStatus(connection, walletId, next);
            wallet.setStatus(next);
            return wallet;
        }, "Не удалось изменить статус кошелька");
    }

    private String nextWalletNumber(Connection connection) throws SQLException {
        int last = walletRepository.findMaxWalletNumber(connection)
                .map(number -> Integer.parseInt(number.substring(4)))
                .orElse(0);
        return String.format("WLT-%06d", last + 1);
    }

    private String nextCardNumber(Connection connection) throws SQLException {
        for (int attempt = 0; attempt < CARD_NUMBER_ATTEMPTS; attempt++) {
            String cardNumber = CardNumbers.generate(ThreadLocalRandom.current());
            if (!walletRepository.existsByCardNumber(connection, cardNumber)) {
                return cardNumber;
            }
        }
        throw new BusinessException("Не удалось выпустить уникальный номер карты, попробуйте ещё раз");
    }
}
