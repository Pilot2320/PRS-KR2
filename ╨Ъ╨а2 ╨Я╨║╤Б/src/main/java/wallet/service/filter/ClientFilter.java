package wallet.service.filter;

import wallet.dto.ClientView;
import wallet.model.WalletStatus;

import java.time.LocalDate;

/** Условия поиска клиентов. Пустое значение (null) означает «не фильтровать по этому полю». */
public record ClientFilter(String query, WalletStatus status, LocalDate registeredFrom, LocalDate registeredTo) {

    public void validate() {
        FilterSupport.checkPeriod(registeredFrom, registeredTo);
    }

    public boolean matches(ClientView client) {
        return FilterSupport.containsText(query, client.fullName(), client.email(), client.phone(), client.walletNumber())
                && (status == null || client.status() == status)
                && FilterSupport.inPeriod(client.registeredAt().toLocalDate(), registeredFrom, registeredTo);
    }
}
