package wallet.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wallet.exception.AppException;
import wallet.exception.BusinessException;

/** Стандартные окна сообщений. Пользователь видит понятный текст, технические подробности уходят в журнал. */
public final class Dialogs {
    private static final Logger log = LoggerFactory.getLogger(Dialogs.class);

    private Dialogs() {
    }

    public static void info(Window owner, String header, String message) {
        create(AlertType.INFORMATION, owner, "Электронный кошелёк", header, message).showAndWait();
    }

    public static void warning(Window owner, String header, String message) {
        create(AlertType.WARNING, owner, "Действие недоступно", header, message).showAndWait();
    }

    public static void error(Window owner, String header, String message) {
        create(AlertType.ERROR, owner, "Ошибка", header, message).showAndWait();
    }

    /** Подтверждение опасного действия. По умолчанию выбрана кнопка «Отмена». */
    public static boolean confirm(Window owner, String header, String message, String confirmText) {
        ButtonType confirm = new ButtonType(confirmText, ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Отмена", ButtonData.CANCEL_CLOSE);
        Alert alert = create(AlertType.CONFIRMATION, owner, "Подтверждение", header, message);
        alert.getButtonTypes().setAll(confirm, cancel);
        Button confirmButton = (Button) alert.getDialogPane().lookupButton(confirm);
        confirmButton.getStyleClass().add("button-danger");
        confirmButton.setDefaultButton(false);
        ((Button) alert.getDialogPane().lookupButton(cancel)).setDefaultButton(true);
        return alert.showAndWait().filter(confirm::equals).isPresent();
    }

    /**
     * Выполняет действие и показывает ошибку, если оно не удалось.
     * Нарушение правила — предупреждение, ошибка базы или файла — окно ошибки.
     */
    public static boolean attempt(Window owner, String header, Runnable action) {
        try {
            action.run();
            return true;
        } catch (BusinessException exception) {
            warning(owner, header, exception.getMessage());
        } catch (AppException exception) {
            error(owner, header, exception.getMessage());
        }
        return false;
    }

    /** Для исключений, которые никто не перехватил: пишем в журнал, пользователю — короткое сообщение. */
    public static void unexpected(Window owner, Throwable error) {
        log.error("Непредвиденная ошибка", error);
        String message = error instanceof AppException
                ? error.getMessage()
                : "Операция не выполнена из-за внутренней ошибки приложения. Подробности записаны в журнал.";
        error(owner, "Что-то пошло не так", message);
    }

    private static Alert create(AlertType type, Window owner, String title, String header, String message) {
        Alert alert = new Alert(type);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(ViewLoader.stylesheet());
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.getDialogPane().setPrefWidth(480);
        return alert;
    }
}
