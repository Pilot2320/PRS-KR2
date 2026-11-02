package wallet.controller;

import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import wallet.dto.WalletView;
import wallet.model.WalletStatus;
import wallet.service.ExportService;
import wallet.service.filter.WalletFilter;
import wallet.ui.Choice;
import wallet.ui.DataModel;
import wallet.ui.Dialogs;
import wallet.ui.ExportDialog;
import wallet.ui.FormDialogs;
import wallet.ui.SortOption;
import wallet.ui.Styles;
import wallet.ui.Tables;
import wallet.util.Formats;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/** Вкладка WalletsTab.fxml: просмотр кошельков, смена статуса, переход к операциям кошелька. */
public class WalletsController {
    @FXML private TableView<WalletView> table;
    @FXML private TableColumn<WalletView, String> numberColumn;
    @FXML private TableColumn<WalletView, String> ownerColumn;
    @FXML private TableColumn<WalletView, String> cardColumn;
    @FXML private TableColumn<WalletView, WalletView> expiryColumn;
    @FXML private TableColumn<WalletView, BigDecimal> balanceColumn;
    @FXML private TableColumn<WalletView, BigDecimal> limitColumn;
    @FXML private TableColumn<WalletView, WalletStatus> statusColumn;
    @FXML private TextField searchField;
    @FXML private ComboBox<Choice<WalletStatus>> statusFilter;
    @FXML private ComboBox<Choice<Boolean>> validityFilter;
    @FXML private ComboBox<SortOption<WalletView>> sortChoice;
    @FXML private Label countLabel;
    @FXML private Button statusButton;
    @FXML private Button operationButton;
    @FXML private Button historyButton;

    private final DataModel dataModel;
    private final ExportService exportService;
    private final FormDialogs forms;
    private final ExportDialog exportDialog;
    private FilteredList<WalletView> filtered;
    private Consumer<WalletView> onShowOperations = wallet -> { };

    public WalletsController(DataModel dataModel, ExportService exportService, FormDialogs forms,
                             ExportDialog exportDialog) {
        this.dataModel = dataModel;
        this.exportService = exportService;
        this.forms = forms;
        this.exportDialog = exportDialog;
    }

    @FXML
    private void initialize() {
        Tables.bind(numberColumn, WalletView::number);
        Tables.bind(ownerColumn, WalletView::ownerName);
        Tables.bind(cardColumn, WalletView::maskedCard);
        Tables.row(expiryColumn,
                wallet -> Formats.month(wallet.expiryDate()) + (wallet.expired() ? " · истёк" : ""),
                wallet -> wallet.expired() ? "text-danger" : null,
                Comparator.comparing(WalletView::expiryDate));
        Tables.bind(balanceColumn, WalletView::balance, Formats::money);
        Tables.bind(limitColumn, WalletView::limitLeft, Formats::money);
        Tables.badge(statusColumn, WalletView::status, WalletStatus::getTitle, Styles::status);

        filtered = new FilteredList<>(dataModel.getWallets());
        SortedList<WalletView> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        Tables.keepSelection(table, WalletView::id);
        Tables.onDoubleClick(table, wallet -> onChangeStatus());

        statusFilter.getItems().setAll(Choice.withAll("Все статусы", List.of(WalletStatus.values())));
        statusFilter.getSelectionModel().selectFirst();
        validityFilter.getItems().setAll(List.of(
                new Choice<>("Любой срок действия", null),
                new Choice<>("Действующие", false),
                new Choice<>("Просроченные", true)));
        validityFilter.getSelectionModel().selectFirst();
        sortChoice.getItems().setAll(List.of(
                new SortOption<>("Номер кошелька", numberColumn, SortType.ASCENDING),
                new SortOption<>("Баланс: сначала больший", balanceColumn, SortType.DESCENDING),
                new SortOption<>("Срок действия: сначала ближайший", expiryColumn, SortType.ASCENDING),
                new SortOption<>("Владелец: от А до Я", ownerColumn, SortType.ASCENDING)));
        sortChoice.valueProperty().addListener((observable, old, option) -> {
            if (option != null) {
                option.applyTo(table);
            }
        });
        sortChoice.getSelectionModel().selectFirst();

        searchField.textProperty().addListener(observable -> applyFilter());
        statusFilter.valueProperty().addListener(observable -> applyFilter());
        validityFilter.valueProperty().addListener(observable -> applyFilter());
        filtered.addListener((ListChangeListener<WalletView>) change -> updateCount());
        dataModel.getWallets().addListener((ListChangeListener<WalletView>) change -> updateCount());

        statusButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        operationButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        historyButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        applyFilter();
    }

    public void setOnShowOperations(Consumer<WalletView> handler) {
        onShowOperations = handler;
    }

    @FXML
    private void onChangeStatus() {
        WalletView wallet = selected();
        if (wallet == null) {
            return;
        }
        if (wallet.status().allowedTransitions().isEmpty()) {
            Dialogs.warning(window(), "Кошелёк " + wallet.number() + " закрыт",
                    "Статус «Закрыт» окончательный: закрытый кошелёк изменить нельзя.");
            return;
        }
        forms.changeWalletStatus(window(), wallet);
    }

    @FXML
    private void onNewOperation() {
        WalletView wallet = selected();
        if (wallet != null) {
            forms.newTransaction(window(), wallet.id());
        }
    }

    @FXML
    private void onShowOperations() {
        WalletView wallet = selected();
        if (wallet != null) {
            onShowOperations.accept(wallet);
        }
    }

    @FXML
    private void onExport() {
        exportDialog.export(window(), exportService.wallets(List.copyOf(table.getItems())), "wallets");
    }

    @FXML
    private void onResetFilters() {
        searchField.clear();
        statusFilter.getSelectionModel().selectFirst();
        validityFilter.getSelectionModel().selectFirst();
        sortChoice.getSelectionModel().selectFirst();
    }

    private void applyFilter() {
        WalletFilter filter = new WalletFilter(searchField.getText(), Choice.valueOf(statusFilter.getValue()),
                Choice.valueOf(validityFilter.getValue()));
        filtered.setPredicate(filter::matches);
    }

    private void updateCount() {
        countLabel.setText("Показано " + filtered.size() + " из "
                + Formats.plural(dataModel.getWallets().size(), "кошелька", "кошельков", "кошельков"));
    }

    private WalletView selected() {
        return table.getSelectionModel().getSelectedItem();
    }

    private Window window() {
        return table.getScene().getWindow();
    }
}
