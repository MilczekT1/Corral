package com.example.consumer.repository;

import java.sql.Connection;
import java.sql.SQLException;

/** JDBC lives here, in the persistence layer. Compliant. */
public class CustomerRepository {
    public void loadAll(Connection connection) throws SQLException {
        connection.createStatement().execute("SELECT * FROM customers");
    }
}
