package wallet.app;

import javafx.application.Application;

/**
 * Точка входа. Отдельный класс без наследования от Application позволяет запускать приложение
 * из IDE и из jar, когда JavaFX подключена через classpath, а не через module-path.
 */
public final class Launcher {
    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(WalletApplication.class, args);
    }
}
