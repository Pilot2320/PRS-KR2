package wallet.service;

import wallet.db.Database;
import wallet.dto.ClientForm;
import wallet.dto.ClientView;
import wallet.exception.BusinessException;
import wallet.exception.ValidationException;
import wallet.model.Client;
import wallet.model.Wallet;
import wallet.repository.ClientRepository;
import wallet.repository.TransactionRepository;
import wallet.repository.WalletRepository;
import wallet.service.validation.ClientValidator;
import wallet.util.Formats;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ClientService {
    private final Database database;
    private final ClientRepository clientRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final WalletService walletService;

    public ClientService(Database database, ClientRepository clientRepository, WalletRepository walletRepository,
                         TransactionRepository transactionRepository, WalletService walletService) {
        this.database = database;
        this.clientRepository = clientRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.walletService = walletService;
    }

    public List<ClientView> findAll() {
        Map<Long, Wallet> walletsByClient = walletRepository.findAll().stream()
                .collect(Collectors.toMap(Wallet::getClientId, Function.identity(), (first, second) -> first));
        return clientRepository.findAll().stream()
                .map(client -> new ClientView(client, walletsByClient.get(client.getId())))
                .toList();
    }

    /** Регистрирует клиента и сразу выпускает ему кошелёк — обе записи в одной транзакции. */
    public ClientView create(ClientForm input) {
        ClientForm form = ClientValidator.normalize(input);
        check(form, null);
        return database.inTransaction(connection -> {
            Client client = clientRepository.insert(connection,
                    new Client(0, form.fullName(), form.email(), form.phone(), LocalDateTime.now()));
            Wallet wallet = walletService.issueFor(connection, client);
            return new ClientView(client, wallet);
        }, "Не удалось зарегистрировать клиента");
    }

    public ClientView update(long id, ClientForm input) {
        ClientForm form = ClientValidator.normalize(input);
        Client client = clientRepository.findById(id).orElseThrow(this::notFound);
        check(form, id);
        client.setFullName(form.fullName());
        client.setEmail(form.email());
        client.setPhone(form.phone());
        if (clientRepository.update(client) == 0) {
            throw notFound();
        }
        return new ClientView(client, walletRepository.findByClientId(id).orElse(null));
    }

    /** Предварительная проверка перед вопросом «Удалить?»: на кошельке не должно быть денег. */
    public void ensureCanDelete(long id) {
        walletRepository.findByClientId(id)
                .filter(wallet -> wallet.getBalance().signum() > 0)
                .ifPresent(wallet -> {
                    throw moneyLeft(wallet);
                });
    }

    /** Удаляет операции кошелька, кошелёк и клиента. Деньги на кошельке должны быть выведены. */
    public void delete(long id) {
        database.runInTransaction(connection -> {
            Optional<Wallet> wallet = walletRepository.findByClientIdForUpdate(connection, id);
            if (wallet.isPresent()) {
                if (wallet.get().getBalance().signum() > 0) {
                    throw moneyLeft(wallet.get());
                }
                transactionRepository.deleteByWalletId(connection, wallet.get().getId());
                walletRepository.delete(connection, wallet.get().getId());
            }
            clientRepository.delete(connection, id);
        }, "Не удалось удалить клиента");
    }

    private void check(ClientForm form, Long exceptId) {
        Map<String, String> errors = ClientValidator.validate(form);
        if (!errors.containsKey(ClientForm.EMAIL)) {
            clientRepository.findByEmail(form.email())
                    .filter(existing -> exceptId == null || existing.getId() != exceptId)
                    .ifPresent(existing -> errors.put(ClientForm.EMAIL,
                            "Этот email уже указан у клиента «" + existing.getFullName() + "». Введите другой адрес"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private BusinessException moneyLeft(Wallet wallet) {
        return new BusinessException("На кошельке " + wallet.getWalletNumber() + " осталось "
                + Formats.money(wallet.getBalance()) + ". Сначала выведите средства, затем удалите клиента");
    }

    private BusinessException notFound() {
        return new BusinessException("Клиент не найден: возможно, его уже удалили. Обновите данные");
    }
}
