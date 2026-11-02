package wallet.repository;

import wallet.model.Client;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

public interface ClientRepository extends Repository<Client> {
    Optional<Client> findByEmail(String email);

    Client insert(Connection connection, Client client) throws SQLException;

    /** @return число изменённых строк: 0 означает, что клиента уже нет в базе. */
    int update(Client client);

    void delete(Connection connection, long id) throws SQLException;
}
