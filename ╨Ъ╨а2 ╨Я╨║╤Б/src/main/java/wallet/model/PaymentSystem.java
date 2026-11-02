package wallet.model;

public enum PaymentSystem {
    MIR("Мир");

    private final String title;

    PaymentSystem(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return title;
    }
}
