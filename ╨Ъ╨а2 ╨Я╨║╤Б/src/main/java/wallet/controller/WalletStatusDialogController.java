package wallet.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import wallet.dto.WalletView;
import wallet.model.Wallet;
import wallet.model.WalletStatus;
import wallet.service.WalletService;
import wallet.ui.FormErrors;
import wallet.ui.Styles;
import wallet.util.Formats;

/** Форма WalletStatusDialog.fxml: в списке только статусы, в которые разрешён переход. */
public class WalletStatusDialogController {
    @FXML private DialogPane dialogPane;
    @FXML private ButtonType saveButtonType;
    @FXML private Label walletValue;
    @FXML private Label ownerValue;
    @FXML private Label balanceValue;
    @FXML private Label currentStatusValue;
    @FXML private ComboBox<WalletStatus> statusBox;
    @FXML private Label statusError;
    @FXML private Label generalError;

    private final WalletService walletService;
    private FormErrors errors;
    private WalletView wallet;
    private Wallet result;

    public WalletStatusDialogController(WalletService walletService) {
        this.walletService = walletService;
    }

    @FXML
    private void initialize() {
        errors = new FormErrors(generalError);
        errors.register(WalletService.STATUS_FIELD, statusBox, statusError);
        Button saveButton = (Button) dialogPane.lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (!save()) {
                event.consume();
            }
        });
    }

    public void edit(WalletView wallet) {
        this.wallet = wallet;
        dialogPane.setHeaderText("Статус кошелька " + wallet.number());
        walletValue.setText(wallet.number() + " · карта " + wallet.maskedCard());
        ownerValue.setText(wallet.ownerName());
        balanceValue.setText(Formats.money(wallet.balance()));
        currentStatusValue.setText(wallet.status().getTitle());
        Styles.setBadge(currentStatusValue, Styles.status(wallet.status()));
        statusBox.getItems().setAll(wallet.status().allowedTransitions());
        statusBox.getSelectionModel().selectFirst();
    }

    public Wallet result() {
        return result;
    }

    private boolean save() {
        result = errors.submit(() -> walletService.changeStatus(wallet.id(), statusBox.getValue())).orElse(null);
        return result != null;
    }
}
