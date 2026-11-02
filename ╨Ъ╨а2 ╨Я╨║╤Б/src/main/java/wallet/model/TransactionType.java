package wallet.model;

public enum TransactionType {
    TOP_UP("Пополнение"),
    TRANSFER("Перевод"),
    WITHDRAWAL("Вывод");

    private final String title;

    TransactionType(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public boolean isOutgoing() {
        return this == TRANSFER || this == WITHDRAWAL;
    }

    @Override
    public String toString() {
        return title;
    }
}
