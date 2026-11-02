package wallet.service.filter;

import wallet.dto.TransactionView;
import wallet.model.TransactionType;

import java.time.LocalDate;

/** Условия поиска операций: текст, тип, кошелёк и период дат. */
public record TransactionFilter(String query, TransactionType type, Long walletId, LocalDate from, LocalDate to) {

    public void validate() {
        FilterSupport.checkPeriod(from, to);
    }

    public boolean matches(TransactionView transaction) {
        return FilterSupport.containsText(query, transaction.description(), transaction.counterparty(),
                transaction.walletNumber(), transaction.ownerName())
                && (type == null || transaction.type() == type)
                && (walletId == null || transaction.walletId() == walletId)
                && FilterSupport.inPeriod(transaction.createdAt().toLocalDate(), from, to);
    }
}
