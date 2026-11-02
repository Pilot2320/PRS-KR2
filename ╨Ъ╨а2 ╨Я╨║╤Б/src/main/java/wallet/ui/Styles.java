package wallet.ui;

import javafx.scene.Node;
import wallet.model.TransactionType;
import wallet.model.WalletStatus;

import java.util.List;

/** Имена CSS-классов для статусов и типов операций (оформление описано в styles.css). */
public final class Styles {
    private static final List<String> BADGES = List.of(
            "badge-active", "badge-blocked", "badge-closed", "badge-top-up", "badge-transfer", "badge-withdrawal");

    private Styles() {
    }

    public static String status(WalletStatus status) {
        return switch (status) {
            case ACTIVE -> "badge-active";
            case BLOCKED -> "badge-blocked";
            case CLOSED -> "badge-closed";
        };
    }

    public static String type(TransactionType type) {
        return switch (type) {
            case TOP_UP -> "badge-top-up";
            case TRANSFER -> "badge-transfer";
            case WITHDRAWAL -> "badge-withdrawal";
        };
    }

    public static void setBadge(Node node, String badgeClass) {
        node.getStyleClass().removeAll(BADGES);
        if (badgeClass != null) {
            node.getStyleClass().add(badgeClass);
        }
    }
}
