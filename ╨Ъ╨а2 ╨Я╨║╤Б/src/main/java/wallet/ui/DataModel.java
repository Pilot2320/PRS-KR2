package wallet.ui;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import wallet.dto.ClientView;
import wallet.dto.TransactionView;
import wallet.dto.WalletView;
import wallet.exception.DatabaseException;
import wallet.model.Client;
import wallet.model.Transaction;
import wallet.model.Wallet;
import wallet.service.ClientService;
import wallet.service.TransactionService;
import wallet.service.WalletService;
import wallet.util.Formats;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Общие для всех вкладок данные. Таблицы показывают эти ObservableList (через FilteredList и SortedList),
 * поэтому после reload() все таблицы, списки и статистика обновляются автоматически.
 */
public class DataModel {
    private final ClientService clientService;
    private final WalletService walletService;
    private final TransactionService transactionService;

    private final ObservableList<ClientView> clients = FXCollections.observableArrayList();
    private final ObservableList<WalletView> wallets = FXCollections.observableArrayList();
    private final ObservableList<TransactionView> transactions = FXCollections.observableArrayList();
    private final ReadOnlyObjectWrapper<LocalDateTime> lastUpdate = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyBooleanWrapper connected = new ReadOnlyBooleanWrapper();
    private final ReadOnlyStringWrapper message = new ReadOnlyStringWrapper("");

    public DataModel(ClientService clientService, WalletService walletService, TransactionService transactionService) {
        this.clientService = clientService;
        this.walletService = walletService;
        this.transactionService = transactionService;
    }

    /** Перечитывает все данные из базы. Списки меняются только после успешной загрузки всех трёх. */
    public void reload() {
        try {
            List<ClientView> loadedClients = clientService.findAll();
            List<WalletView> loadedWallets = walletService.findAll();
            List<TransactionView> loadedTransactions = transactionService.findAll();
            clients.setAll(loadedClients);
            wallets.setAll(loadedWallets);
            transactions.setAll(loadedTransactions);
            connected.set(true);
            lastUpdate.set(LocalDateTime.now());
        } catch (DatabaseException exception) {
            connected.set(false);
            throw exception;
        }
    }

    public void refresh(String doneMessage) {
        reload();
        setMessage(doneMessage);
    }

    public void setMessage(String text) {
        message.set(Formats.time(LocalDateTime.now()) + " · " + text);
    }

    public Optional<WalletView> findWallet(long walletId) {
        return wallets.stream().filter(wallet -> wallet.id() == walletId).findFirst();
    }

    public List<TransactionView> transactionsOf(long walletId) {
        return transactions.stream().filter(transaction -> transaction.walletId() == walletId).toList();
    }

    public List<Client> clientEntities() {
        return clients.stream().map(ClientView::client).toList();
    }

    public List<Wallet> walletEntities() {
        return wallets.stream().map(WalletView::wallet).toList();
    }

    public List<Transaction> transactionEntities() {
        return transactions.stream().map(TransactionView::transaction).toList();
    }

    public ObservableList<ClientView> getClients() {
        return clients;
    }

    public ObservableList<WalletView> getWallets() {
        return wallets;
    }

    public ObservableList<TransactionView> getTransactions() {
        return transactions;
    }

    public ReadOnlyObjectProperty<LocalDateTime> lastUpdateProperty() {
        return lastUpdate.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty connectedProperty() {
        return connected.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty messageProperty() {
        return message.getReadOnlyProperty();
    }
}
