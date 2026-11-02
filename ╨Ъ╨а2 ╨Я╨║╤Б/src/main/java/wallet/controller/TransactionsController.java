package wallet.controller;

import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import wallet.dto.TransactionView;
import wallet.dto.WalletView;
import wallet.exception.BusinessException;
import wallet.model.TransactionType;
import wallet.service.ExportService;
import wallet.service.StatisticsService;
import wallet.service.filter.TransactionFilter;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Вкладка TransactionsTab.fxml: журнал операций с поиском, фильтрами по типу, кошельку и периоду. */
public class TransactionsController {
    @FXML private TableView<TransactionView> table;
    @FXML private TableColumn<TransactionView, LocalDateTime> dateColumn;
    @FXML private TableColumn<TransactionView, String> walletColumn;
    @FXML private TableColumn<TransactionView, String> ownerColumn;
    @FXML private TableColumn<TransactionView, TransactionType> typeColumn;
    @FXML private TableColumn<TransactionView, TransactionView> amountColumn;
    @FXML private TableColumn<TransactionView, String> descriptionColumn;
    @FXML private TableColumn<TransactionView, String> counterpartyColumn;
    @FXML private TextField searchField;
    @FXML private ComboBox<Choice<TransactionType>> typeFilter;
    @FXML private ComboBox<Choice<Long>> walletFilter;
    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private ComboBox<SortOption<TransactionView>> sortChoice;
    @FXML private Label filterError;
    @FXML private Label countLabel;
    @FXML private Label totalsLabel;
    @FXML private Button detailsButton;

    private final DataModel dataModel;
    private final ExportService exportService;
    private final FormDialogs forms;
    private final ExportDialog exportDialog;
    private FilteredList<TransactionView> filtered;

    public TransactionsController(DataModel dataModel, ExportService exportService, FormDialogs forms,
                                  ExportDialog exportDialog) {
        this.dataModel = dataModel;
        this.exportService = exportService;
        this.forms = forms;
        this.exportDialog = exportDialog;
    }

    @FXML
    private void initialize() {
        Tables.bind(dateColumn, TransactionView::createdAt, Formats::dateTime);
        Tables.bind(walletColumn, TransactionView::walletNumber);
        Tables.bind(ownerColumn, TransactionView::ownerName);
        Tables.badge(typeColumn, TransactionView::type, TransactionType::getTitle, Styles::type);
        Tables.row(amountColumn,
                operation -> Formats.signedMoney(operation.amount(), operation.type().isOutgoing()),
                operation -> operation.type().isOutgoing() ? "amount-out" : "amount-in",
                Comparator.comparing(TransactionView::amount));
        Tables.bind(descriptionColumn, TransactionView::description);
        Tables.bind(counterpartyColumn, TransactionView::counterparty);

        filtered = new FilteredList<>(dataModel.getTransactions());
        SortedList<TransactionView> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        Tables.keepSelection(table, TransactionView::id);
        Tables.onDoubleClick(table, this::showDetails);

        typeFilter.getItems().setAll(Choice.withAll("Все типы", List.of(TransactionType.values())));
        typeFilter.getSelectionModel().selectFirst();
        rebuildWalletChoices();
        dataModel.getWallets().addListener((ListChangeListener<WalletView>) change -> rebuildWalletChoices());
        fromDate.setConverter(Converters.date());
        toDate.setConverter(Converters.date());
        sortChoice.getItems().setAll(List.of(
                new SortOption<>("Дата: сначала новые", dateColumn, SortType.DESCENDING),
                new SortOption<>("Дата: сначала старые", dateColumn, SortType.ASCENDING),
                new SortOption<>("Сумма: сначала большие", amountColumn, SortType.DESCENDING),
                new SortOption<>("Сумма: сначала маленькие", amountColumn, SortType.ASCENDING)));
        sortChoice.valueProperty().addListener((observable, old, option) -> {
            if (option != null) {
                option.applyTo(table);
            }
        });
        sortChoice.getSelectionModel().selectFirst();

        searchField.textProperty().addListener(observable -> applyFilter());
        typeFilter.valueProperty().addListener(observable -> applyFilter());
        walletFilter.valueProperty().addListener(observable -> applyFilter());
        fromDate.valueProperty().addListener(observable -> applyFilter());
        toDate.valueProperty().addListener(observable -> applyFilter());
        filtered.addListener((ListChangeListener<TransactionView>) change -> updateSummary());
        dataModel.getTransactions().addListener((ListChangeListener<TransactionView>) change -> updateSummary());

        detailsButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        applyFilter();
    }

    /** Открывает журнал только по одному кошельку (переход со вкладки «Кошельки»). */
    public void showWallet(long walletId) {
        onResetFilters();
        walletFilter.getItems().stream()
                .filter(choice -> Objects.equals(choice.value(), walletId))
                .findFirst()
                .ifPresent(walletFilter::setValue);
    }

    @FXML
    private void onNewOperation() {
        forms.newTransaction(window(), Choice.valueOf(walletFilter.getValue()))
                .ifPresent(created -> Tables.select(table, operation -> operation.id() == created.getId()));
    }

    @FXML
    private void onShowDetails() {
        TransactionView operation = table.getSelectionModel().getSelectedItem();
        if (operation != null) {
            showDetails(operation);
        }
    }

    @FXML
    private void onExport() {
        exportDialog.export(window(), exportService.transactions(List.copyOf(table.getItems())), "transactions");
    }

    @FXML
    private void onResetFilters() {
        searchField.clear();
        typeFilter.getSelectionModel().selectFirst();
        walletFilter.getSelectionModel().selectFirst();
        fromDate.setValue(null);
        toDate.setValue(null);
        sortChoice.getSelectionModel().selectFirst();
    }

    private void applyFilter() {
        TransactionFilter filter = new TransactionFilter(searchField.getText(), Choice.valueOf(typeFilter.getValue()),
                Choice.valueOf(walletFilter.getValue()), fromDate.getValue(), toDate.getValue());
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

    private void rebuildWalletChoices() {
        Long selectedId = Choice.valueOf(walletFilter.getValue());
        List<Choice<Long>> choices = new ArrayList<>();
        choices.add(new Choice<>("Все кошельки", null));
        dataModel.getWallets().forEach(wallet -> choices.add(new Choice<>(wallet.label(), wallet.id())));
        walletFilter.getItems().setAll(choices);
        walletFilter.setValue(choices.stream()
                .filter(choice -> Objects.equals(choice.value(), selectedId))
                .findFirst()
                .orElse(choices.get(0)));
    }

    private void updateSummary() {
        countLabel.setText("Показано " + filtered.size() + " из "
                + Formats.plural(dataModel.getTransactions().size(), "операции", "операций", "операций"));
        StatisticsService.Turnover turnover = StatisticsService.turnover(
                filtered.stream().map(TransactionView::transaction).toList());
        totalsLabel.setText("В выборке: поступления " + Formats.signedMoney(turnover.income(), false)
                + " · списания " + Formats.signedMoney(turnover.outcome(), true));
    }

    private void showDetails(TransactionView operation) {
        String text = String.join("\n",
                "Тип: " + operation.type().getTitle(),
                "Сумма: " + Formats.money(operation.amount()),
                "Кошелёк: " + operation.walletNumber() + " (" + operation.ownerName() + ")",
                "Получатель: " + (operation.counterparty().isBlank() ? Formats.EMPTY : operation.counterparty()),
                "Описание: " + (operation.description().isBlank() ? Formats.EMPTY : operation.description()));
        Dialogs.info(window(), "Операция от " + Formats.dateTime(operation.createdAt()), text);
    }

    private Window window() {
        return table.getScene().getWindow();
    }
}
