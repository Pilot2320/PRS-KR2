package wallet.app;

import wallet.controller.ClientDialogController;
import wallet.controller.ClientsController;
import wallet.controller.MainController;
import wallet.controller.StatisticsController;
import wallet.controller.TransactionDialogController;
import wallet.controller.TransactionsController;
import wallet.controller.WalletStatusDialogController;
import wallet.controller.WalletsController;
import wallet.db.Database;
import wallet.db.DatabaseConfig;
import wallet.export.CsvExporter;
import wallet.export.ExcelExporter;
import wallet.repository.ClientRepository;
import wallet.repository.JdbcClientRepository;
import wallet.repository.JdbcTransactionRepository;
import wallet.repository.JdbcWalletRepository;
import wallet.repository.TransactionRepository;
import wallet.repository.WalletRepository;
import wallet.service.ClientService;
import wallet.service.ExportService;
import wallet.service.StatisticsService;
import wallet.service.TransactionService;
import wallet.service.WalletService;
import wallet.ui.DataModel;
import wallet.ui.ExportDialog;
import wallet.ui.FormDialogs;
import wallet.ui.ViewLoader;

import java.util.List;

/**
 * Сборка приложения: база → репозитории → сервисы → модель интерфейса.
 * FXMLLoader создаёт контроллеры через createController и получает их уже с нужными зависимостями.
 */
public class AppContext {
    private final Database database;
    private final ClientService clientService;
    private final WalletService walletService;
    private final TransactionService transactionService;
    private final StatisticsService statisticsService;
    private final ExportService exportService;
    private final DataModel dataModel;
    private final ViewLoader views;
    private final FormDialogs forms;
    private final ExportDialog exportDialog;

    public AppContext() {
        database = new Database(DatabaseConfig.load());
        ClientRepository clientRepository = new JdbcClientRepository(database);
        WalletRepository walletRepository = new JdbcWalletRepository(database);
        TransactionRepository transactionRepository = new JdbcTransactionRepository(database);

        walletService = new WalletService(database, walletRepository, clientRepository, transactionRepository);
        clientService = new ClientService(database, clientRepository, walletRepository, transactionRepository, walletService);
        transactionService = new TransactionService(database, transactionRepository, walletRepository, clientRepository);
        statisticsService = new StatisticsService();
        exportService = new ExportService(List.of(new CsvExporter(), new ExcelExporter()));

        dataModel = new DataModel(clientService, walletService, transactionService);
        views = new ViewLoader(this::createController);
        forms = new FormDialogs(views, dataModel);
        exportDialog = new ExportDialog(exportService);
    }

    public ViewLoader views() {
        return views;
    }

    private Object createController(Class<?> type) {
        if (type == MainController.class) {
            return new MainController(dataModel, database.displayName());
        }
        if (type == ClientsController.class) {
            return new ClientsController(dataModel, clientService, exportService, forms, exportDialog);
        }
        if (type == WalletsController.class) {
            return new WalletsController(dataModel, exportService, forms, exportDialog);
        }
        if (type == TransactionsController.class) {
            return new TransactionsController(dataModel, exportService, forms, exportDialog);
        }
        if (type == StatisticsController.class) {
            return new StatisticsController(dataModel, statisticsService, exportService, exportDialog);
        }
        if (type == ClientDialogController.class) {
            return new ClientDialogController(clientService);
        }
        if (type == WalletStatusDialogController.class) {
            return new WalletStatusDialogController(walletService);
        }
        if (type == TransactionDialogController.class) {
            return new TransactionDialogController(transactionService);
        }
        throw new IllegalArgumentException("Неизвестный контроллер: " + type.getName());
    }
}
