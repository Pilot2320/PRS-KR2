package wallet.db;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface SqlAction {
    void run(Connection connection) throws SQLException;
}
