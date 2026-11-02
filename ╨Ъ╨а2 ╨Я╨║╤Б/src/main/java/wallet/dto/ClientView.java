package wallet.dto;

import wallet.model.Client;
import wallet.model.Wallet;
import wallet.model.WalletStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Строка таблицы «Клиенты»: клиент вместе с его кошельком (кошелька может не быть). */
public record ClientView(Client client, Wallet wallet) {

    public long id() {
        return client.getId();
    }

    public String fullName() {
        return client.getFullName();
    }

    public String email() {
        return client.getEmail();
    }

    public String phone() {
        return client.getPhone();
    }

    public LocalDateTime registeredAt() {
        return client.getRegisteredAt();
    }

    public boolean hasWallet() {
        return wallet != null;
    }

    public String walletNumber() {
        return wallet == null ? "" : wallet.getWalletNumber();
    }

    public BigDecimal balance() {
        return wallet == null ? null : wallet.getBalance();
    }

    public WalletStatus status() {
        return wallet == null ? null : wallet.getStatus();
    }
}
