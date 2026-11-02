package wallet.service.filter;

import wallet.dto.WalletView;
import wallet.model.WalletStatus;

/** Условия поиска кошельков. expired: null — любой срок, true — только просроченные, false — только действующие. */
public record WalletFilter(String query, WalletStatus status, Boolean expired) {

    public boolean matches(WalletView wallet) {
        return FilterSupport.containsText(query, wallet.number(), wallet.ownerName(), wallet.holderName(),
                wallet.wallet().cardLastDigits())
                && (status == null || wallet.status() == status)
                && (expired == null || wallet.expired() == expired);
    }
}
