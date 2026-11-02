package wallet.repository;

import wallet.model.Wallet;
import wallet.model.WalletStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

public interface WalletRepository extends Repository<Wallet> {
    Optional<Wallet> findByClientId(long clientId);

    /** Читает кошелёк и блокирует строку (SELECT ... FOR UPDATE) до конца транзакции. */
    Optional<Wallet> findByIdForUpdate(Connection connection, long id) throws SQLException;

    Optional<Wallet> findByClientIdForUpdate(Connection connection, long clientId) throws SQLException;

    Optional<String> findMaxWalletNumber(Connection connection) throws SQLException;

    boolean existsByCardNumber(Connection connection, String cardNumber) throws SQLException;

    Wallet insert(Connection connection, Wallet wallet) throws SQLException;

    void updateStatus(Connection connection, long id, WalletStatus status) throws SQLException;

    void updateBalance(Connection connection, long id, BigDecimal balance) throws SQLException;

    void delete(Connection connection, long id) throws SQLException;
}
