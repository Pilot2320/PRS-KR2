package wallet.repository;

import wallet.model.Transaction;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;

public interface TransactionRepository extends Repository<Transaction> {
    Transaction insert(Connection connection, Transaction transaction) throws SQLException;

    void deleteByWalletId(Connection connection, long walletId) throws SQLException;

    BigDecimal sumTransfers(Connection connection, long walletId, LocalDateTime from, LocalDateTime to)
            throws SQLException;

    /** Сумма переводов за период по каждому кошельку: ID кошелька → сумма. */
    Map<Long, BigDecimal> sumTransfersByWallet(LocalDateTime from, LocalDateTime to);
}
