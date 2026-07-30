package com.example.consumer.service;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Pre-existing technical debt: raw JDBC in a service package.
 *
 * <p>Deliberately left in place. It is seeded into the committed freeze store on first run, which
 * demonstrates the "freeze, don't block" principle — adopting the rules does not force a
 * repository-wide cleanup before the build can go green.
 */
public class ReportService {
    public void render(Connection connection) throws SQLException {
        connection.createStatement().execute("SELECT count(*) FROM orders");
    }
}
