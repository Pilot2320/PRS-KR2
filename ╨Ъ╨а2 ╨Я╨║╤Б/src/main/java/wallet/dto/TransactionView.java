package wallet.dto;

import wallet.model.Client;
import wallet.model.Transaction;
import wallet.model.TransactionType;
import wallet.model.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Строка таблицы «Операции»: операция, её кошелёк и владелец кошелька. */
public record TransactionView(Transaction transaction, Wallet wallet, Client owner) {

    public long id() {
        return transaction.getId();
    }

    public long walletId() {
        return transaction.getWalletId();
    }

    public LocalDateTime createdAt() {
        return transaction.getCreatedAt();
    }

    public TransactionType type() {
        return transaction.getType();
    }

    public BigDecimal amount() {
        return transaction.getAmount();
    }

    public String description() {
        return transaction.getDescription() == null ? "" : transaction.getDescription();
    }

    public String counterparty() {
        return transaction.getCounterparty() == null ? "" : transaction.getCounterparty();
    }

    public String walletNumber() {
        return wallet == null ? "—" : wallet.getWalletNumber();
    }

    public String ownerName() {
        return owner == null ? "—" : owner.getFullName();
    }
}
