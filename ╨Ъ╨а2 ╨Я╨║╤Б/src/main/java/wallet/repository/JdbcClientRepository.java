package wallet.repository;

import wallet.db.Database;
import wallet.model.Client;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class JdbcClientRepository extends JdbcRepository<Client> implements ClientRepository {
    private static final String SELECT = "SELECT id, full_name, email, phone, registered_at FROM clients";

    public JdbcClientRepository(Database database) {
        super(database);
    }

    @Override
    public List<Client> findAll() {
        return database.read(connection -> queryList(connection, SELECT + " ORDER BY id"),
                "Не удалось загрузить клиентов");
    }

    @Override
    public Optional<Client> findById(long id) {
        return database.read(connection -> queryOne(connection, SELECT + " WHERE id = ?", id),
                "Не удалось найти клиента");
    }

    @Override
    public Optional<Client> findByEmail(String email) {
        return database.read(connection -> queryOne(connection, SELECT + " WHERE LOWER(email) = LOWER(?)", email),
                "Не удалось проверить email");
    }

    @Override
    public Client insert(Connection connection, Client client) throws SQLException {
        long id = insertReturningId(connection,
                "INSERT INTO clients (full_name, email, phone, registered_at) VALUES (?, ?, ?, ?) RETURNING id",
                client.getFullName(), client.getEmail(), client.getPhone(), client.getRegisteredAt());
        client.setId(id);
        return client;
    }

    @Override
    public int update(Client client) {
        return database.read(connection -> execute(connection,
                        "UPDATE clients SET full_name = ?, email = ?, phone = ? WHERE id = ?",
                        client.getFullName(), client.getEmail(), client.getPhone(), client.getId()),
                "Не удалось сохранить изменения клиента");
    }

    @Override
    public void delete(Connection connection, long id) throws SQLException {
        execute(connection, "DELETE FROM clients WHERE id = ?", id);
    }

    @Override
    protected Client map(ResultSet resultSet) throws SQLException {
        return new Client(
                resultSet.getLong("id"),
                resultSet.getString("full_name"),
                resultSet.getString("email"),
                resultSet.getString("phone"),
                resultSet.getObject("registered_at", LocalDateTime.class));
    }
}
