package io.github.milczekt1.corral.rules.security.nojdbcstatement.fixtures;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * MUST IGNORE when imported from where it compiles, test output: building a fixture schema is the
 * idiom there. MUST FLAG if copied into a production layout, which is how the test shows the scope
 * clause, not the call, is what spares it.
 */
public class SchemaBuilder {

    public void create(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE orders (id BIGINT PRIMARY KEY)");
        }
    }
}
