package io.github.milczekt1.corral.rules.security.nojdbcstatement.fixtures;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * MUST FLAG the {@code createStatement} call in {@code byCustomer}, once. MUST IGNORE the
 * {@code prepareStatement} call in {@code byId}: same owner, so a predicate dropping the name fails.
 */
public class OrderDao {

    public boolean byCustomer(Connection connection, String customer) throws SQLException {
        Statement statement = connection.createStatement();
        try (ResultSet rows = statement.executeQuery("SELECT * FROM orders WHERE customer = '" + customer + "'")) {
            return rows.next();
        }
    }

    public boolean byId(Connection connection, long id) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT * FROM orders WHERE id = ?");
        statement.setLong(1, id);
        try (ResultSet rows = statement.executeQuery()) {
            return rows.next();
        }
    }
}
