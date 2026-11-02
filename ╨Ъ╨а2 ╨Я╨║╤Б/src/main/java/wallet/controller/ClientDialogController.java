package wallet.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import wallet.dto.ClientForm;
import wallet.dto.ClientView;
import wallet.service.ClientService;
import wallet.ui.FormErrors;
import wallet.ui.Nodes;

/** Форма ClientDialog.fxml: регистрация нового клиента или изменение существующего. */
public class ClientDialogController {
    @FXML private DialogPane dialogPane;
    @FXML private ButtonType saveButtonType;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private Label nameError;
    @FXML private Label emailError;
    @FXML private Label phoneError;
    @FXML private Label walletNote;
    @FXML private Label generalError;

    private final ClientService clientService;
    private FormErrors errors;
    private ClientView editing;
    private ClientView result;

    public ClientDialogController(ClientService clientService) {
        this.clientService = clientService;
    }

    @FXML
    private void initialize() {
        errors = new FormErrors(generalError);
        errors.register(ClientForm.FULL_NAME, nameField, nameError);
        errors.register(ClientForm.EMAIL, emailField, emailError);
        errors.register(ClientForm.PHONE, phoneField, phoneError);

        // Окно закрывается только после успешного сохранения: при ошибке событие кнопки поглощается.
        Button saveButton = (Button) dialogPane.lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (!save()) {
                event.consume();
            }
        });
        Platform.runLater(nameField::requestFocus);
    }

    /** @param client клиент для изменения; null — регистрация нового. */
    public void edit(ClientView client) {
        editing = client;
        Nodes.setShown(walletNote, client == null);
        if (client == null) {
            dialogPane.setHeaderText("Регистрация нового клиента");
            return;
        }
        dialogPane.setHeaderText("Изменение данных клиента");
        nameField.setText(client.fullName());
        emailField.setText(client.email());
        phoneField.setText(client.phone());
    }

    public ClientView result() {
        return result;
    }

    private boolean save() {
        ClientForm form = new ClientForm(nameField.getText(), emailField.getText(), phoneField.getText());
        result = errors.submit(() -> editing == null
                        ? clientService.create(form)
                        : clientService.update(editing.id(), form))
                .orElse(null);
        return result != null;
    }
}
