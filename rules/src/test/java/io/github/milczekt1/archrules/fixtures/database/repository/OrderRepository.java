package io.github.milczekt1.archrules.fixtures.database.repository;

import java.sql.Connection;
import java.sql.SQLException;

/** Raw JDBC INSIDE a repository package — allowed. */
public class OrderRepository {
    public void findAll(Connection connection) throws SQLException {
        connection.createStatement().execute("SELECT * FROM orders");
    }
}
