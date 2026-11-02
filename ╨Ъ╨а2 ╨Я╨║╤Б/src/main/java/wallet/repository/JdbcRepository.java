package wallet.repository;

import wallet.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Общий код JDBC-репозиториев. Значения подставляются в запрос только через параметры «?»
 * PreparedStatement, поэтому пользовательский ввод не может изменить текст SQL.
 */
abstract class JdbcRepository<T> {
    protected final Database database;

    protected JdbcRepository(Database database) {
        this.database = database;
    }

    protected abstract T map(ResultSet resultSet) throws SQLException;

    protected List<T> queryList(Connection connection, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = prepare(connection, sql, parameters);
             ResultSet resultSet = statement.executeQuery()) {
            List<T> rows = new ArrayList<>();
            while (resultSet.next()) {
                rows.add(map(resultSet));
            }
            return rows;
        }
    }

    protected Optional<T> queryOne(Connection connection, String sql, Object... parameters) throws SQLException {
        List<T> rows = queryList(connection, sql, parameters);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    protected long insertReturningId(Connection connection, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = prepare(connection, sql, parameters);
             ResultSet keys = statement.executeQuery()) {
            if (!keys.next()) {
                throw new SQLException("База данных не вернула ID новой записи");
            }
            return keys.getLong(1);
        }
    }

    protected int execute(Connection connection, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = prepare(connection, sql, parameters)) {
            return statement.executeUpdate();
        }
    }

    protected static PreparedStatement prepare(Connection connection, String sql, Object... parameters)
            throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        try {
            for (int index = 0; index < parameters.length; index++) {
                statement.setObject(index + 1, parameters[index]);
            }
            return statement;
        } catch (SQLException exception) {
            statement.close();
            throw exception;
        }
    }
}
