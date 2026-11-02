package wallet.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import wallet.ui.DataModel;
import wallet.ui.Dialogs;
import wallet.util.Formats;

/**
 * Главное окно MainView.fxml. Вкладки подключены через fx:include, их контроллеры
 * FXMLLoader внедряет в поля с суффиксом Controller (walletsView → walletsViewController).
 */
public class MainController {
    @FXML private TabPane tabPane;
    @FXML private Tab transactionsTab;
    @FXML private Label connectionLabel;
    @FXML private Label summaryLabel;
    @FXML private Label messageLabel;
    @FXML private WalletsController walletsViewController;
    @FXML private TransactionsController transactionsViewController;

    private final DataModel dataModel;
    private final String databaseName;

    public MainController(DataModel dataModel, String databaseName) {
        this.dataModel = dataModel;
        this.databaseName = databaseName;
    }

    @FXML
    private void initialize() {
        walletsViewController.setOnShowOperations(wallet -> {
            tabPane.getSelectionModel().select(transactionsTab);
            transactionsViewController.showWallet(wallet.id());
        });
        dataModel.connectedProperty().addListener(observable -> updateConnection());
        dataModel.lastUpdateProperty().addListener(observable -> updateSummary());
        messageLabel.textProperty().bind(dataModel.messageProperty());
    }

    /** Первая загрузка и кнопка «Обновить» (F5): перечитать все данные из PostgreSQL. */
    @FXML
    public void loadData() {
        Dialogs.attempt(tabPane.getScene().getWindow(), "Не удалось загрузить данные",
                () -> dataModel.refresh("Данные загружены из базы " + databaseName));
        updateConnection();
    }

    private void updateConnection() {
        boolean connected = dataModel.connectedProperty().get();
        connectionLabel.setText(connected ? "● PostgreSQL · " + databaseName : "● Нет подключения к базе данных");
        connectionLabel.getStyleClass().removeAll("connection-ok", "connection-error");
        connectionLabel.getStyleClass().add(connected ? "connection-ok" : "connection-error");
    }

    private void updateSummary() {
        summaryLabel.setText(
                Formats.plural(dataModel.getClients().size(), "клиент", "клиента", "клиентов") + " · "
                        + Formats.plural(dataModel.getWallets().size(), "кошелёк", "кошелька", "кошельков") + " · "
                        + Formats.plural(dataModel.getTransactions().size(), "операция", "операции", "операций")
                        + " · обновлено в " + Formats.time(dataModel.lastUpdateProperty().get()));
    }
}
