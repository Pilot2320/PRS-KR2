package wallet.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/**
 * Загружает FXML из ресурсов /wallet/view/. Контроллеры создаёт фабрика приложения,
 * поэтому в их конструкторы попадают нужные сервисы.
 */
public class ViewLoader {
    private static final String VIEWS = "/wallet/view/";

    private final Callback<Class<?>, Object> controllerFactory;

    public ViewLoader(Callback<Class<?>, Object> controllerFactory) {
        this.controllerFactory = controllerFactory;
    }

    public <C> View<C> load(String fxml) {
        URL location = ViewLoader.class.getResource(VIEWS + fxml);
        if (location == null) {
            throw new IllegalStateException("Не найден файл интерфейса " + VIEWS + fxml);
        }
        FXMLLoader loader = new FXMLLoader(location);
        loader.setControllerFactory(controllerFactory);
        try {
            Parent root = loader.load();
            return new View<>(root, loader.getController());
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось загрузить интерфейс " + fxml, exception);
        }
    }

    public static String stylesheet() {
        return Objects.requireNonNull(ViewLoader.class.getResource(VIEWS + "styles.css")).toExternalForm();
    }

    public record View<C>(Parent root, C controller) {
    }
}
