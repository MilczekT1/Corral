package io.github.milczekt1.corral.rules.security.nojdbcstatement.fixtures;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MUST IGNORE: the value is bound, never concatenated. {@code executeQuery} on a
 * {@code PreparedStatement} is a {@code Statement} method, so a predicate matching on the
 * {@code Statement} type rather than the {@code Connection} call fails here.
 */
public class PaymentDao {

    public boolean byId(Connection connection, long id) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT * FROM payments WHERE id = ?");
        statement.setLong(1, id);
        try (ResultSet rows = statement.executeQuery()) {
            return rows.next();
        }
    }
}
