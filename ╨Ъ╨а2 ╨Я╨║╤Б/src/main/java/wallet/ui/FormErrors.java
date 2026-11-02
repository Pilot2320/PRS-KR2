package wallet.ui;

import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import wallet.exception.AppException;
import wallet.exception.ValidationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Показ ошибок в форме: текст под полем и красная рамка у самого поля.
 * Ошибка поля исчезает, как только пользователь начинает его исправлять.
 */
public class FormErrors {
    private final Label generalLabel;
    private final Map<String, Control> controls = new LinkedHashMap<>();
    private final Map<String, Label> labels = new HashMap<>();

    public FormErrors(Label generalLabel) {
        this.generalLabel = generalLabel;
        Nodes.setShown(generalLabel, false);
    }

    public void register(String field, Control control, Label errorLabel) {
        controls.put(field, control);
        labels.put(field, errorLabel);
        Nodes.setShown(errorLabel, false);
        if (control instanceof TextInputControl input) {
            input.textProperty().addListener(observable -> clear(field));
        } else if (control instanceof ComboBoxBase<?> box) {
            box.valueProperty().addListener(observable -> clear(field));
        }
    }

    /** Выполняет сохранение. При ошибке показывает её в форме и возвращает пустой результат. */
    public <T> Optional<T> submit(Supplier<T> action) {
        clearAll();
        try {
            return Optional.ofNullable(action.get());
        } catch (ValidationException exception) {
            show(exception.getErrors());
        } catch (AppException exception) {
            showGeneral(exception.getMessage());
        }
        Nodes.fitWindow(generalLabel);
        return Optional.empty();
    }

    private void show(Map<String, String> errors) {
        Control firstInvalid = null;
        List<String> otherErrors = new ArrayList<>();
        for (Map.Entry<String, String> error : errors.entrySet()) {
            Label label = labels.get(error.getKey());
            if (label == null) {
                otherErrors.add(error.getValue());
                continue;
            }
            label.setText(error.getValue());
            Nodes.setShown(label, true);
            Control control = controls.get(error.getKey());
            Nodes.markInvalid(true, control);
            if (firstInvalid == null) {
                firstInvalid = control;
            }
        }
        if (!otherErrors.isEmpty()) {
            showGeneral(String.join("\n", otherErrors));
        }
        if (firstInvalid != null) {
            firstInvalid.requestFocus();
        }
    }

    private void showGeneral(String message) {
        generalLabel.setText(message);
        Nodes.setShown(generalLabel, true);
    }

    private void clear(String field) {
        Nodes.setShown(labels.get(field), false);
        Nodes.markInvalid(false, controls.get(field));
    }

    private void clearAll() {
        controls.keySet().forEach(this::clear);
        Nodes.setShown(generalLabel, false);
    }
}
