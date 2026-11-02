package wallet.ui;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;

public final class Nodes {
    public static final String INVALID = "field-invalid";

    private Nodes() {
    }

    /** Скрытый узел не занимает места в разметке (managed = false). */
    public static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    public static void showMessage(Label label, String message) {
        label.setText(message);
        setShown(label, true);
    }

    public static void markInvalid(boolean invalid, Node... nodes) {
        for (Node node : nodes) {
            node.getStyleClass().remove(INVALID);
            if (invalid) {
                node.getStyleClass().add(INVALID);
            }
        }
    }

    /**
     * Окно диалога не меняет размер само, когда в форме появляются или скрываются строки.
     * Размер пересчитывается после ближайшей раскладки, когда известна высота перенесённого текста.
     */
    public static void fitWindow(Node node) {
        Platform.runLater(() -> {
            Scene scene = node.getScene();
            if (scene != null && scene.getWindow() != null) {
                scene.getWindow().sizeToScene();
            }
        });
    }
}
