package wallet.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wallet.exception.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * JDBC-подключение к PostgreSQL. Каждая операция открывает своё соединение и закрывает его
 * через try-with-resources. Изменения из нескольких запросов выполняются в одной транзакции.
 */
public class Database {
    private static final Logger log = LoggerFactory.getLogger(Database.class);
    private static final int LOGIN_TIMEOUT_SECONDS = 5;

    private final DatabaseConfig config;

    public Database(DatabaseConfig config) {
        this.config = config;
        DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SECONDS);
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(config.url(), config.user(), config.password());
    }

    public String displayName() {
        return config.displayName();
    }

    public <T> T read(SqlFunction<T> action, String errorMessage) {
        try (Connection connection = getConnection()) {
            return action.apply(connection);
        } catch (SQLException exception) {
            log.error(errorMessage, exception);
            throw DatabaseException.of(errorMessage, exception);
        }
    }

    /** Выполняет действие в транзакции: commit при успехе, rollback при любой ошибке. */
    public <T> T inTransaction(SqlFunction<T> action, String errorMessage) {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = action.apply(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            log.error(errorMessage, exception);
            throw DatabaseException.of(errorMessage, exception);
        }
    }

    public void runInTransaction(SqlAction action, String errorMessage) {
        inTransaction(connection -> {
            action.run(connection);
            return null;
        }, errorMessage);
    }
}
