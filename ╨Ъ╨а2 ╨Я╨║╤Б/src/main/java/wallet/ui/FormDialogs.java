package wallet.ui;

import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;
import wallet.controller.ClientDialogController;
import wallet.controller.TransactionDialogController;
import wallet.controller.WalletStatusDialogController;
import wallet.dto.ClientView;
import wallet.dto.WalletView;
import wallet.model.Transaction;
import wallet.model.TransactionType;
import wallet.model.Wallet;
import wallet.util.Formats;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Открывает модальные формы (FXML + контроллер) и после успешного сохранения обновляет данные.
 * Формы вызываются из разных вкладок, поэтому код открытия собран здесь, а не повторяется в контроллерах.
 */
public class FormDialogs {
    private final ViewLoader views;
    private final DataModel dataModel;

    public FormDialogs(ViewLoader views, DataModel dataModel) {
        this.views = views;
        this.dataModel = dataModel;
    }

    public Optional<ClientView> createClient(Window owner) {
        Optional<ClientView> saved = clientForm(owner, null);
        saved.ifPresent(client -> refresh(owner, "Клиент «" + client.fullName() + "» зарегистрирован, выпущен кошелёк "
                + client.walletNumber()));
        return saved;
    }

    public Optional<ClientView> editClient(Window owner, ClientView client) {
        Optional<ClientView> saved = clientForm(owner, client);
        saved.ifPresent(updated -> refresh(owner, "Данные клиента «" + updated.fullName() + "» сохранены"));
        return saved;
    }

    public Optional<Wallet> changeWalletStatus(Window owner, WalletView wallet) {
        ViewLoader.View<WalletStatusDialogController> view = views.load("WalletStatusDialog.fxml");
        view.controller().edit(wallet);
        Optional<Wallet> saved = show(owner, view, "Статус кошелька", view.controller()::result);
        saved.ifPresent(updated -> refresh(owner, "Кошелёк " + updated.getWalletNumber() + ": статус «"
                + updated.getStatus().getTitle() + "»"));
        return saved;
    }

    /** @param walletId кошелёк, который будет выбран в форме заранее; null — выбрать вручную. */
    public Optional<Transaction> newTransaction(Window owner, Long walletId) {
        ViewLoader.View<TransactionDialogController> view = views.load("TransactionDialog.fxml");
        view.controller().setWallets(dataModel.getWallets(), walletId);
        Optional<Transaction> saved = show(owner, view, "Новая операция", view.controller()::result);
        saved.ifPresent(transaction -> refresh(owner, describe(transaction)));
        return saved;
    }

    private Optional<ClientView> clientForm(Window owner, ClientView client) {
        ViewLoader.View<ClientDialogController> view = views.load("ClientDialog.fxml");
        view.controller().edit(client);
        return show(owner, view, client == null ? "Новый клиент" : "Изменение клиента", view.controller()::result);
    }

    private <T> Optional<T> show(Window owner, ViewLoader.View<?> view, String title, Supplier<T> result) {
        Dialog<T> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setDialogPane((DialogPane) view.root());
        dialog.setResizable(true);
        dialog.setResultConverter(button -> button != null && button.getButtonData() == ButtonData.OK_DONE
                ? result.get()
                : null);
        return dialog.showAndWait();
    }

    private void refresh(Window owner, String message) {
        Dialogs.attempt(owner, "Данные сохранены, но не обновились на экране", () -> dataModel.refresh(message));
    }

    private String describe(Transaction transaction) {
        String wallet = dataModel.findWallet(transaction.getWalletId()).map(WalletView::number).orElse("");
        String text = transaction.getType().getTitle() + " " + Formats.money(transaction.getAmount()) + ", кошелёк " + wallet;
        if (transaction.getType() == TransactionType.TRANSFER) {
            text += ", получатель: " + transaction.getCounterparty();
        }
        return "Операция проведена: " + text;
    }
}
