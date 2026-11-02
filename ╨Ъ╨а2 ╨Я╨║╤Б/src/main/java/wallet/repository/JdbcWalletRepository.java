package wallet.repository;

import wallet.db.Database;
import wallet.model.Currency;
import wallet.model.PaymentSystem;
import wallet.model.Wallet;
import wallet.model.WalletStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class JdbcWalletRepository extends JdbcRepository<Wallet> implements WalletRepository {
    private static final String SELECT = """
            SELECT id, client_id, wallet_number, card_number, holder_name, payment_system,
                   expiry_date, currency, balance, status, daily_limit, created_at
            FROM wallets""";

    public JdbcWalletRepository(Database database) {
        super(database);
    }

    @Override
    public List<Wallet> findAll() {
        return database.read(connection -> queryList(connection, SELECT + " ORDER BY wallet_number"),
                "Не удалось загрузить кошельки");
    }

    @Override
    public Optional<Wallet> findById(long id) {
        return database.read(connection -> queryOne(connection, SELECT + " WHERE id = ?", id),
                "Не удалось найти кошелёк");
    }

    @Override
    public Optional<Wallet> findByClientId(long clientId) {
        return database.read(connection -> queryOne(connection, SELECT + " WHERE client_id = ?", clientId),
                "Не удалось найти кошелёк клиента");
    }

    @Override
    public Optional<Wallet> findByIdForUpdate(Connection connection, long id) throws SQLException {
        return queryOne(connection, SELECT + " WHERE id = ? FOR UPDATE", id);
    }

    @Override
    public Optional<Wallet> findByClientIdForUpdate(Connection connection, long clientId) throws SQLException {
        return queryOne(connection, SELECT + " WHERE client_id = ? FOR UPDATE", clientId);
    }

    @Override
    public Optional<String> findMaxWalletNumber(Connection connection) throws SQLException {
        String sql = "SELECT MAX(wallet_number) FROM wallets WHERE wallet_number ~ '^WLT-[0-9]{6}$'";
        try (PreparedStatement statement = prepare(connection, sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return Optional.ofNullable(resultSet.getString(1));
        }
    }

    @Override
    public boolean existsByCardNumber(Connection connection, String cardNumber) throws SQLException {
        try (PreparedStatement statement = prepare(connection, "SELECT 1 FROM wallets WHERE card_number = ?", cardNumber);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next();
        }
    }

    @Override
    public Wallet insert(Connection connection, Wallet wallet) throws SQLException {
        long id = insertReturningId(connection, """
                        INSERT INTO wallets (client_id, wallet_number, card_number, holder_name, payment_system,
                                             expiry_date, currency, balance, status, daily_limit, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id""",
                wallet.getClientId(), wallet.getWalletNumber(), wallet.getCardNumber(), wallet.getHolderName(),
                wallet.getPaymentSystem().name(), wallet.getExpiryDate(), wallet.getCurrency().name(),
                wallet.getBalance(), wallet.getStatus().name(), wallet.getDailyLimit(), wallet.getCreatedAt());
        wallet.setId(id);
        return wallet;
    }

    @Override
    public void updateStatus(Connection connection, long id, WalletStatus status) throws SQLException {
        execute(connection, "UPDATE wallets SET status = ? WHERE id = ?", status.name(), id);
    }

    @Override
    public void updateBalance(Connection connection, long id, BigDecimal balance) throws SQLException {
        execute(connection, "UPDATE wallets SET balance = ? WHERE id = ?", balance, id);
    }

    @Override
    public void delete(Connection connection, long id) throws SQLException {
        execute(connection, "DELETE FROM wallets WHERE id = ?", id);
    }

    @Override
    protected Wallet map(ResultSet resultSet) throws SQLException {
        Wallet wallet = new Wallet();
        wallet.setId(resultSet.getLong("id"));
        wallet.setClientId(resultSet.getLong("client_id"));
        wallet.setWalletNumber(resultSet.getString("wallet_number"));
        wallet.setCardNumber(resultSet.getString("card_number"));
        wallet.setHolderName(resultSet.getString("holder_name"));
        wallet.setPaymentSystem(PaymentSystem.valueOf(resultSet.getString("payment_system")));
        wallet.setExpiryDate(resultSet.getObject("expiry_date", LocalDate.class));
        wallet.setCurrency(Currency.valueOf(resultSet.getString("currency")));
        wallet.setBalance(resultSet.getBigDecimal("balance"));
        wallet.setStatus(WalletStatus.valueOf(resultSet.getString("status")));
        wallet.setDailyLimit(resultSet.getBigDecimal("daily_limit"));
        wallet.setCreatedAt(resultSet.getObject("created_at", LocalDateTime.class));
        return wallet;
    }
}
