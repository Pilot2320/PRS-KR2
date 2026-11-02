package wallet.repository;

import wallet.db.Database;
import wallet.model.Transaction;
import wallet.model.TransactionType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcTransactionRepository extends JdbcRepository<Transaction> implements TransactionRepository {
    private static final String SELECT =
            "SELECT id, wallet_id, type, amount, description, counterparty, created_at FROM transactions";

    public JdbcTransactionRepository(Database database) {
        super(database);
    }

    @Override
    public List<Transaction> findAll() {
        return database.read(connection -> queryList(connection, SELECT + " ORDER BY created_at DESC, id DESC"),
                "Не удалось загрузить операции");
    }

    @Override
    public Optional<Transaction> findById(long id) {
        return database.read(connection -> queryOne(connection, SELECT + " WHERE id = ?", id),
                "Не удалось найти операцию");
    }

    @Override
    public Transaction insert(Connection connection, Transaction transaction) throws SQLException {
        long id = insertReturningId(connection, """
                        INSERT INTO transactions (wallet_id, type, amount, description, counterparty, created_at)
                        VALUES (?, ?, ?, ?, ?, ?) RETURNING id""",
                transaction.getWalletId(), transaction.getType().name(), transaction.getAmount(),
                transaction.getDescription(), transaction.getCounterparty(), transaction.getCreatedAt());
        transaction.setId(id);
        return transaction;
    }

    @Override
    public void deleteByWalletId(Connection connection, long walletId) throws SQLException {
        execute(connection, "DELETE FROM transactions WHERE wallet_id = ?", walletId);
    }

    @Override
    public BigDecimal sumTransfers(Connection connection, long walletId, LocalDateTime from, LocalDateTime to)
            throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(amount), 0) FROM transactions
                WHERE wallet_id = ? AND type = 'TRANSFER' AND created_at >= ? AND created_at < ?""";
        try (PreparedStatement statement = prepare(connection, sql, walletId, from, to);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getBigDecimal(1);
        }
    }

    @Override
    public Map<Long, BigDecimal> sumTransfersByWallet(LocalDateTime from, LocalDateTime to) {
        String sql = """
                SELECT wallet_id, SUM(amount) FROM transactions
                WHERE type = 'TRANSFER' AND created_at >= ? AND created_at < ?
                GROUP BY wallet_id""";
        return database.read(connection -> {
            try (PreparedStatement statement = prepare(connection, sql, from, to);
                 ResultSet resultSet = statement.executeQuery()) {
                Map<Long, BigDecimal> sums = new HashMap<>();
                while (resultSet.next()) {
                    sums.put(resultSet.getLong(1), resultSet.getBigDecimal(2));
                }
                return sums;
            }
        }, "Не удалось посчитать дневные лимиты");
    }

    @Override
    protected Transaction map(ResultSet resultSet) throws SQLException {
        Transaction transaction = new Transaction();
        transaction.setId(resultSet.getLong("id"));
        transaction.setWalletId(resultSet.getLong("wallet_id"));
        transaction.setType(TransactionType.valueOf(resultSet.getString("type")));
        transaction.setAmount(resultSet.getBigDecimal("amount"));
        transaction.setDescription(resultSet.getString("description"));
        transaction.setCounterparty(resultSet.getString("counterparty"));
        transaction.setCreatedAt(resultSet.getObject("created_at", LocalDateTime.class));
        return transaction;
    }
}
