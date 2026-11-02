package wallet.controller;

import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import wallet.dto.ClientView;
import wallet.dto.TransactionView;
import wallet.exception.BusinessException;
import wallet.model.Wallet;
import wallet.model.WalletStatus;
import wallet.service.ClientService;
import wallet.service.ExportService;
import wallet.service.filter.ClientFilter;
import wallet.ui.Choice;
import wallet.ui.Converters;
import wallet.ui.DataModel;
import wallet.ui.Dialogs;
import wallet.ui.ExportDialog;
import wallet.ui.FormDialogs;
import wallet.ui.Nodes;
import wallet.ui.SortOption;
import wallet.ui.Styles;
import wallet.ui.Tables;
import wallet.util.Formats;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Вкладка ClientsTab.fxml: основная сущность — клиент с его кошельком. Полный CRUD. */
public class ClientsController {
    @FXML private TableView<ClientView> table;
    @FXML private TableColumn<ClientView, String> nameColumn;
    @FXML private TableColumn<ClientView, String> emailColumn;
    @FXML private TableColumn<ClientView, String> phoneColumn;
    @FXML private TableColumn<ClientView, LocalDateTime> registeredColumn;
    @FXML private TableColumn<ClientView, String> walletColumn;
    @FXML private TableColumn<ClientView, BigDecimal> balanceColumn;
    @FXML private TableColumn<ClientView, WalletStatus> statusColumn;
    @FXML private TextField searchField;
    @FXML private ComboBox<Choice<WalletStatus>> statusFilter;
    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private ComboBox<SortOption<ClientView>> sortChoice;
    @FXML private Label filterError;
    @FXML private Label countLabel;
    @FXML private Button editButton;
    @FXML private Button deleteButton;

    @FXML private Label detailsPlaceholder;
    @FXML private VBox details;
    @FXML private Label nameValue;
    @FXML private Label emailValue;
    @FXML private Label phoneValue;
    @FXML private Label registeredValue;
    @FXML private VBox walletCard;
    @FXML private Label cardStatusLabel;
    @FXML private Label cardNumberLabel;
    @FXML private Label cardHolderLabel;
    @FXML private Label cardExpiryLabel;
    @FXML private Label cardBalanceLabel;
    @FXML private Label cardWalletNumberLabel;
    @FXML private GridPane walletInfo;
    @FXML private Label limitValue;
    @FXML private Label noWalletLabel;
    @FXML private Label operationsTitle;
    @FXML private ListView<TransactionView> operationsList;

    private final DataModel dataModel;
    private final ClientService clientService;
    private final ExportService exportService;
    private final FormDialogs forms;
    private final ExportDialog exportDialog;
    private FilteredList<ClientView> filtered;

    public ClientsController(DataModel dataModel, ClientService clientService, ExportService exportService,
                             FormDialogs forms, ExportDialog exportDialog) {
        this.dataModel = dataModel;
        this.clientService = clientService;
        this.exportService = exportService;
        this.forms = forms;
        this.exportDialog = exportDialog;
    }

    @FXML
    private void initialize() {
        Tables.bind(nameColumn, ClientView::fullName);
        Tables.bind(emailColumn, ClientView::email);
        Tables.bind(phoneColumn, ClientView::phone);
        Tables.bind(registeredColumn, ClientView::registeredAt, Formats::dateOnly);
        Tables.bind(walletColumn, ClientView::walletNumber);
        Tables.bind(balanceColumn, ClientView::balance, Formats::money);
        Tables.badge(statusColumn, ClientView::status, WalletStatus::getTitle, Styles::status);

        // ObservableList → FilteredList (поиск и фильтры) → SortedList (сортировка) → TableView.
        filtered = new FilteredList<>(dataModel.getClients());
        SortedList<ClientView> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        Tables.keepSelection(table, ClientView::id);
        Tables.onDoubleClick(table, client -> onEdit());
        table.setOnKeyPressed(event -> {
            if (Tables.isDeleteKey(event)) {
                onDelete();
            }
        });

        statusFilter.getItems().setAll(Choice.withAll("Все статусы кошелька", List.of(WalletStatus.values())));
        statusFilter.getSelectionModel().selectFirst();
        fromDate.setConverter(Converters.date());
        toDate.setConverter(Converters.date());
        sortChoice.getItems().setAll(List.of(
                new SortOption<>("ФИО: от А до Я", nameColumn, SortType.ASCENDING),
                new SortOption<>("Регистрация: сначала новые", registeredColumn, SortType.DESCENDING),
                new SortOption<>("Баланс: сначала больший", balanceColumn, SortType.DESCENDING)));
        sortChoice.valueProperty().addListener((observable, old, option) -> {
            if (option != null) {
                option.applyTo(table);
            }
        });
        sortChoice.getSelectionModel().selectFirst();

        searchField.textProperty().addListener(observable -> applyFilter());
        statusFilter.valueProperty().addListener(observable -> applyFilter());
        fromDate.valueProperty().addListener(observable -> applyFilter());
        toDate.valueProperty().addListener(observable -> applyFilter());
        filtered.addListener((ListChangeListener<ClientView>) change -> updateCount());
        dataModel.getClients().addListener((ListChangeListener<ClientView>) change -> updateCount());

        editButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        table.getSelectionModel().selectedItemProperty().addListener((observable, old, client) -> showDetails(client));
        dataModel.lastUpdateProperty().addListener(observable -> showDetails(selected()));
        operationsList.setCellFactory(list -> new OperationCell());

        applyFilter();
        showDetails(null);
    }

    @FXML
    private void onAdd() {
        forms.createClient(window()).ifPresent(client -> select(client.id()));
    }

    @FXML
    private void onEdit() {
        ClientView client = selected();
        if (client != null) {
            forms.editClient(window(), client).ifPresent(updated -> select(updated.id()));
        }
    }

    @FXML
    private void onDelete() {
        ClientView client = selected();
        if (client == null) {
            return;
        }
        Window owner = window();
        if (!Dialogs.attempt(owner, "Удалить клиента нельзя", () -> clientService.ensureCanDelete(client.id()))) {
            return;
        }
        String consequences = client.hasWallet()
                ? "Вместе с клиентом будут удалены кошелёк " + client.walletNumber() + " и "
                + Formats.plural(dataModel.transactionsOf(client.wallet().getId()).size(), "операция", "операции", "операций")
                + " по нему."
                : "У клиента нет кошелька.";
        if (!Dialogs.confirm(owner, "Удалить клиента «" + client.fullName() + "»?",
                consequences + "\nДействие нельзя отменить.", "Удалить")) {
            return;
        }
        if (Dialogs.attempt(owner, "Не удалось удалить клиента", () -> clientService.delete(client.id()))) {
            Dialogs.attempt(owner, "Не удалось обновить данные",
                    () -> dataModel.refresh("Клиент «" + client.fullName() + "» удалён"));
        }
    }

    @FXML
    private void onExport() {
        exportDialog.export(window(), exportService.clients(List.copyOf(table.getItems())), "clients");
    }

    @FXML
    private void onResetFilters() {
        searchField.clear();
        statusFilter.getSelectionModel().selectFirst();
        fromDate.setValue(null);
        toDate.setValue(null);
        sortChoice.getSelectionModel().selectFirst();
    }

    private void applyFilter() {
        ClientFilter filter = new ClientFilter(searchField.getText(), Choice.valueOf(statusFilter.getValue()),
                fromDate.getValue(), toDate.getValue());
        try {
            filter.validate();
            filtered.setPredicate(filter::matches);
            Nodes.setShown(filterError, false);
            Nodes.markInvalid(false, fromDate, toDate);
        } catch (BusinessException exception) {
            Nodes.showMessage(filterError, exception.getMessage());
            Nodes.markInvalid(true, fromDate, toDate);
        }
    }

    private void updateCount() {
        countLabel.setText("Показано " + filtered.size() + " из "
                + Formats.plural(dataModel.getClients().size(), "клиента", "клиентов", "клиентов"));
    }

    private void showDetails(ClientView client) {
        Nodes.setShown(detailsPlaceholder, client == null);
        Nodes.setShown(details, client != null);
        if (client == null) {
            return;
        }
        nameValue.setText(client.fullName());
        emailValue.setText(client.email());
        phoneValue.setText(client.phone());
        registeredValue.setText(Formats.dateTime(client.registeredAt()));

        Wallet wallet = client.wallet();
        Nodes.setShown(walletCard, wallet != null);
        Nodes.setShown(walletInfo, wallet != null);
        Nodes.setShown(noWalletLabel, wallet == null);
        if (wallet == null) {
            operationsTitle.setText("Операции");
            operationsList.getItems().clear();
            return;
        }
        cardStatusLabel.setText(wallet.getStatus().getTitle());
        Styles.setBadge(cardStatusLabel, Styles.status(wallet.getStatus()));
        cardNumberLabel.setText("••••  ••••  ••••  " + wallet.cardLastDigits());
        cardHolderLabel.setText(wallet.getHolderName());
        cardExpiryLabel.setText((wallet.isExpired(LocalDate.now()) ? "истёк " : "до ") + Formats.month(wallet.getExpiryDate()));
        cardBalanceLabel.setText(Formats.money(wallet.getBalance()));
        cardWalletNumberLabel.setText(wallet.getWalletNumber());
        limitValue.setText(dataModel.findWallet(wallet.getId())
                .map(view -> Formats.money(view.limitLeft()) + " из " + Formats.money(wallet.getDailyLimit()))
                .orElse(Formats.EMPTY));

        List<TransactionView> operations = dataModel.transactionsOf(wallet.getId());
        operationsTitle.setText("Операции по кошельку · " + operations.size());
        operationsList.getItems().setAll(operations);
    }

    private void select(long clientId) {
        if (!Tables.select(table, client -> client.id() == clientId)) {
            onResetFilters();
            Tables.select(table, client -> client.id() == clientId);
        }
    }

    private ClientView selected() {
        return table.getSelectionModel().getSelectedItem();
    }

    private Window window() {
        return table.getScene().getWindow();
    }

    /** Строка списка операций в карточке клиента: тип и сумма, ниже дата и описание. */
    private static final class OperationCell extends ListCell<TransactionView> {
        @Override
        protected void updateItem(TransactionView operation, boolean empty) {
            super.updateItem(operation, empty);
            setText(null);
            if (empty || operation == null) {
                setGraphic(null);
                return;
            }
            boolean outgoing = operation.type().isOutgoing();
            Label title = new Label(operation.type().getTitle() + "   " + Formats.signedMoney(operation.amount(), outgoing));
            title.getStyleClass().add(outgoing ? "amount-out" : "amount-in");
            String description = operation.description().isBlank() ? operation.counterparty() : operation.description();
            Label meta = new Label(Formats.dateTime(operation.createdAt())
                    + (description.isBlank() ? "" : " · " + description));
            meta.getStyleClass().add("muted");
            setGraphic(new VBox(2, title, meta));
        }
    }
}
