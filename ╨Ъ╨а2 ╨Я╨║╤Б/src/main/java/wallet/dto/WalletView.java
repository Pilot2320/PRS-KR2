package wallet.dto;

import wallet.model.Client;
import wallet.model.Wallet;
import wallet.model.WalletStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Строка таблицы «Кошельки»: кошелёк, его владелец и остаток дневного лимита переводов на сегодня. */
public record WalletView(Wallet wallet, Client owner, BigDecimal limitLeft) {

    public long id() {
        return wallet.getId();
    }

    public String number() {
        return wallet.getWalletNumber();
    }

    public String ownerName() {
        return owner == null ? "—" : owner.getFullName();
    }

    public String holderName() {
        return wallet.getHolderName();
    }

    public String maskedCard() {
        return wallet.maskedCardNumber();
    }

    public LocalDate expiryDate() {
        return wallet.getExpiryDate();
    }

    public boolean expired() {
        return wallet.isExpired(LocalDate.now());
    }

    public BigDecimal balance() {
        return wallet.getBalance();
    }

    public WalletStatus status() {
        return wallet.getStatus();
    }

    public String label() {
        return number() + " · " + ownerName();
    }
}
