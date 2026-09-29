package io.github.milczekt1.corral.rules.dependencies.nojavax.fixtures;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

/** MUST FLAG: {@code javax.sql} is a JDK package that never moved, and the whole namespace is banned. */
public class DataSourceHealthCheck {

    private final DataSource dataSource;

    public DataSourceHealthCheck(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean isReachable() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(1);
        }
    }
}
