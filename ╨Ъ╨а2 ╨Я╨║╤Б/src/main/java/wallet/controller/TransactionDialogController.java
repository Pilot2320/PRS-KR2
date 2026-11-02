package wallet.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import wallet.dto.TransactionForm;
import wallet.dto.WalletView;
import wallet.model.Transaction;
import wallet.model.TransactionType;
import wallet.model.WalletStatus;
import wallet.service.TransactionService;
import wallet.service.validation.TransactionValidator;
import wallet.ui.Converters;
import wallet.ui.FormErrors;
import wallet.ui.Nodes;
import wallet.util.Formats;

import java.util.List;
import java.util.Locale;

/**
 * Форма TransactionDialog.fxml. Кошельки выбираются из списка, а не вводятся по ID.
 * Поле «Получатель» показывается только для перевода.
 */
public class TransactionDialogController {
    @FXML private DialogPane dialogPane;
    @FXML private ButtonType saveButtonType;
    @FXML private ComboBox<WalletView> walletBox;
    @FXML private Label walletInfo;
    @FXML private Label walletError;
    @FXML private ComboBox<TransactionType> typeBox;
    @FXML private Label typeHint;
    @FXML private Label typeError;
    @FXML private TextField amountField;
    @FXML private Label amountError;
    @FXML private VBox targetBlock;
    @FXML private ComboBox<WalletView> targetBox;
    @FXML private Label targetInfo;
    @FXML private Label targetError;
    @FXML private TextArea descriptionArea;
    @FXML private Label descriptionCounter;
    @FXML private Label descriptionError;
    @FXML private Label generalError;

    private final TransactionService transactionService;
    private FormErrors errors;
    private List<WalletView> wallets = List.of();
    private Transaction result;

    public TransactionDialogController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @FXML
    private void initialize() {
        walletBox.setConverter(Converters.of(this::walletLabel));
        targetBox.setConverter(Converters.of(this::walletLabel));
        typeBox.getItems().setAll(TransactionType.values());
        typeBox.setValue(TransactionType.TOP_UP);

        errors = new FormErrors(generalError);
        errors.register(TransactionForm.WALLET, walletBox, walletError);
        errors.register(TransactionForm.TYPE, typeBox, typeError);
        errors.register(TransactionForm.AMOUNT, amountField, amountError);
        errors.register(TransactionForm.TARGET, targetBox, targetError);
        errors.register(TransactionForm.DESCRIPTION, descriptionArea, descriptionError);

        walletBox.valueProperty().addListener(observable -> onWalletChanged());
        typeBox.valueProperty().addListener(observable -> onTypeChanged());
        targetBox.valueProperty().addListener(observable -> showInfo(targetInfo, targetBox.getValue()));
        descriptionArea.textProperty().addListener(observable -> updateCounter());
        showInfo(walletInfo, null);
        showInfo(targetInfo, null);

        Button saveButton = (Button) dialogPane.lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (!save()) {
                event.consume();
            }
        });
        dialogPane.setHeaderText("Пополнение, перевод или вывод средств");
        onTypeChanged();
        updateCounter();
    }

    public void setWallets(List<WalletView> allWallets, Long preselectedId) {
        wallets = List.copyOf(allWallets);
        walletBox.getItems().setAll(wallets);
        if (preselectedId != null) {
            wallets.stream().filter(wallet -> wallet.id() == preselectedId).findFirst().ifPresent(walletBox::setValue);
        }
        Platform.runLater(() -> (walletBox.getValue() == null ? walletBox : amountField).requestFocus());
    }

    public Transaction result() {
        return result;
    }

    private void onWalletChanged() {
        WalletView source = walletBox.getValue();
        showInfo(walletInfo, source);
        WalletView target = targetBox.getValue();
        targetBox.getItems().setAll(wallets.stream()
                .filter(wallet -> source == null || wallet.id() != source.id())
                .toList());
        targetBox.setValue(target != null && targetBox.getItems().contains(target) ? target : null);
    }

    private void onTypeChanged() {
        TransactionType type = typeBox.getValue();
        Nodes.setShown(targetBlock, type == TransactionType.TRANSFER);
        typeHint.setText(type == null ? "" : switch (type) {
            case TOP_UP -> "Сумма зачисляется на кошелёк. Дневной лимит не расходуется.";
            case TRANSFER -> "Сумма списывается и зачисляется получателю. Переводы за день — не больше 100 000 ₽.";
            case WITHDRAWAL -> "Сумма списывается с кошелька на внешний счёт. Дневной лимит не расходуется.";
        });
        Nodes.fitWindow(dialogPane);
    }

    private void updateCounter() {
        String text = descriptionArea.getText();
        int length = text == null ? 0 : text.trim().length();
        descriptionCounter.setText(length + " / " + TransactionValidator.MAX_DESCRIPTION_LENGTH);
        Nodes.markInvalid(length > TransactionValidator.MAX_DESCRIPTION_LENGTH, descriptionCounter);
    }

    private String walletLabel(WalletView wallet) {
        String label = wallet.number() + " · " + wallet.ownerName();
        if (wallet.status() != WalletStatus.ACTIVE) {
            return label + " (" + wallet.status().getTitle().toLowerCase(Locale.ROOT) + ")";
        }
        return wallet.expired() ? label + " (просрочен)" : label;
    }

    /** Подсказка под выбранным кошельком: баланс, остаток лимита и статус. Без выбора строка скрыта. */
    private void showInfo(Label label, WalletView wallet) {
        Nodes.setShown(label, wallet != null);
        if (wallet == null) {
            return;
        }
        String state = wallet.status().getTitle() + (wallet.expired() ? ", срок действия истёк" : "");
        label.setText("Баланс " + Formats.money(wallet.balance()) + " · можно перевести сегодня "
                + Formats.money(wallet.limitLeft()) + " · " + state);
    }

    private boolean save() {
        WalletView source = walletBox.getValue();
        WalletView target = targetBox.getValue();
        TransactionType type = typeBox.getValue();
        TransactionForm form = new TransactionForm(
                source == null ? null : source.id(),
                type,
                amountField.getText(),
                descriptionArea.getText(),
                type == TransactionType.TRANSFER && target != null ? target.id() : null);
        result = errors.submit(() -> transactionService.create(form)).orElse(null);
        return result != null;
    }
}
