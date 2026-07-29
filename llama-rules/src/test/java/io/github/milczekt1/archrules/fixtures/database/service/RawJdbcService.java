package io.github.milczekt1.archrules.fixtures.database.service;

import java.sql.Connection;
import java.sql.SQLException;

/** Raw JDBC outside a repository/dao/jdbc package — a violation. */
public class RawJdbcService {
    public void query(Connection connection) throws SQLException {
        connection.createStatement().execute("SELECT 1");
    }
}
