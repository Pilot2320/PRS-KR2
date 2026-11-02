package wallet.model;

import java.util.Arrays;
import java.util.List;

public enum WalletStatus {
    ACTIVE("Активен"),
    BLOCKED("Заблокирован"),
    CLOSED("Закрыт");

    private final String title;

    WalletStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    /** Активен → Заблокирован/Закрыт, Заблокирован → Активен/Закрыт, Закрыт — окончательный статус. */
    public boolean canTransitTo(WalletStatus next) {
        return switch (this) {
            case ACTIVE -> next == BLOCKED || next == CLOSED;
            case BLOCKED -> next == ACTIVE || next == CLOSED;
            case CLOSED -> false;
        };
    }

    public List<WalletStatus> allowedTransitions() {
        return Arrays.stream(values()).filter(this::canTransitTo).toList();
    }

    @Override
    public String toString() {
        return title;
    }
}
