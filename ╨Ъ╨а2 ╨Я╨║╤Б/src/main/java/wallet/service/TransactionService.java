package wallet.service;

import wallet.db.Database;
import wallet.dto.TransactionForm;
import wallet.dto.TransactionView;
import wallet.exception.ValidationException;
import wallet.model.Client;
import wallet.model.Transaction;
import wallet.model.TransactionType;
import wallet.model.Wallet;
import wallet.model.WalletStatus;
import wallet.repository.ClientRepository;
import wallet.repository.TransactionRepository;
import wallet.repository.WalletRepository;
import wallet.service.validation.TransactionValidator;
import wallet.util.Formats;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TransactionService {
    private final Database database;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final ClientRepository clientRepository;

    public TransactionService(Database database, TransactionRepository transactionRepository,
                              WalletRepository walletRepository, ClientRepository clientRepository) {
        this.database = database;
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.clientRepository = clientRepository;
    }

    public List<TransactionView> findAll() {
        Map<Long, Wallet> wallets = walletRepository.findAll().stream()
                .collect(Collectors.toMap(Wallet::getId, Function.identity()));
        Map<Long, Client> owners = clientRepository.findAll().stream()
                .collect(Collectors.toMap(Client::getId, Function.identity()));
        return transactionRepository.findAll().stream()
                .map(transaction -> {
                    Wallet wallet = wallets.get(transaction.getWalletId());
                    Client owner = wallet == null ? null : owners.get(wallet.getClientId());
                    return new TransactionView(transaction, wallet, owner);
                })
                .toList();
    }

    /**
     * Проводит операцию и меняет баланс в одной транзакции базы данных.
     * Кошельки блокируются (SELECT ... FOR UPDATE), чтобы параллельная операция не изменила баланс между
     * проверкой и записью.
     */
    public Transaction create(TransactionForm form) {
        TransactionValidator.Checked checked = TransactionValidator.validate(form);
        Transaction transaction = new Transaction();
        transaction.setWalletId(form.walletId());
        transaction.setType(form.type());
        transaction.setAmount(checked.amount());
        transaction.setDescription(checked.description());
        transaction.setCounterparty("");
        transaction.setCreatedAt(LocalDateTime.now());

        return database.inTransaction(connection -> {
            boolean transfer = form.type() == TransactionType.TRANSFER;
            Wallet source;
            Wallet target = null;
            if (transfer) {
                long sourceId = form.walletId();
                long targetId = form.targetWalletId();
                // Строки блокируются по возрастанию ID: два встречных перевода не заблокируют друг друга.
                Wallet first = lock(connection, Math.min(sourceId, targetId), sourceId);
                Wallet second = lock(connection, Math.max(sourceId, targetId), sourceId);
                source = first.getId() == sourceId ? first : second;
                target = first.getId() == targetId ? first : second;
            } else {
                source = lock(connection, form.walletId(), form.walletId());
            }

            ensureCanMoveMoney(source, false);
            if (target != null) {
                ensureCanMoveMoney(target, true);
                transaction.setCounterparty("Кошелёк " + target.getWalletNumber());
            }

            BigDecimal amount = checked.amount();
            BigDecimal nextBalance = form.type() == TransactionType.TOP_UP
                    ? source.getBalance().add(amount)
                    : source.getBalance().subtract(amount);
            if (nextBalance.signum() < 0) {
                throw ValidationException.of(TransactionForm.AMOUNT, "Недостаточно средств: на кошельке "
                        + Formats.money(source.getBalance()) + ", а списать нужно " + Formats.money(amount));
            }
            if (transfer) {
                LocalDateTime dayStart = LocalDate.now().atStartOfDay();
                BigDecimal spent = transactionRepository.sumTransfers(connection, source.getId(), dayStart, dayStart.plusDays(1));
                BigDecimal left = WalletService.limitLeft(source, spent);
                if (amount.compareTo(left) > 0) {
                    throw ValidationException.of(TransactionForm.AMOUNT, "Превышен дневной лимит переводов: сегодня можно перевести ещё "
                            + Formats.money(left));
                }
            }

            walletRepository.updateBalance(connection, source.getId(), nextBalance);
            if (target != null) {
                walletRepository.updateBalance(connection, target.getId(), target.getBalance().add(amount));
            }
            return transactionRepository.insert(connection, transaction);
        }, "Не удалось провести операцию");
    }

    private Wallet lock(Connection connection, long walletId, long sourceId) throws SQLException {
        boolean recipient = walletId != sourceId;
        return walletRepository.findByIdForUpdate(connection, walletId)
                .orElseThrow(() -> ValidationException.of(
                        recipient ? TransactionForm.TARGET : TransactionForm.WALLET,
                        (recipient ? "Кошелёк получателя" : "Кошелёк") + " не найден: обновите данные"));
    }

    private void ensureCanMoveMoney(Wallet wallet, boolean recipient) {
        String field = recipient ? TransactionForm.TARGET : TransactionForm.WALLET;
        String who = (recipient ? "Кошелёк получателя " : "Кошелёк ") + wallet.getWalletNumber();
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw ValidationException.of(field, who + " в статусе «" + wallet.getStatus().getTitle()
                    + "»: операции доступны только для активных кошельков");
        }
        if (wallet.isExpired(LocalDate.now())) {
            throw ValidationException.of(field, who + " просрочен: он действовал до конца "
                    + Formats.month(wallet.getExpiryDate()));
        }
    }
}
