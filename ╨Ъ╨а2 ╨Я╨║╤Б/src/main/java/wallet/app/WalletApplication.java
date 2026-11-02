package wallet.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.stage.Stage;
import wallet.controller.MainController;
import wallet.ui.Dialogs;
import wallet.ui.ViewLoader;

/**
 * Stage — окно приложения, Scene — содержимое окна, Node — любой элемент интерфейса внутри сцены.
 * Корневой Node главного окна загружается из MainView.fxml.
 */
public class WalletApplication extends Application {
    private AppContext context;

    @Override
    public void init() {
        context = new AppContext();
    }

    @Override
    public void start(Stage stage) {
        Thread.currentThread().setUncaughtExceptionHandler((thread, error) -> Dialogs.unexpected(stage, error));

        ViewLoader.View<MainController> main = context.views().load("MainView.fxml");
        Scene scene = new Scene(main.root(), 1320, 820);
        scene.getStylesheets().add(ViewLoader.stylesheet());
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F5), main.controller()::loadData);

        stage.setTitle("Электронный кошелёк — КР №2");
        stage.setMinWidth(1100);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();

        main.controller().loadData();
    }
}
