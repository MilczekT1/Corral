package io.github.milczekt1.corral.rules.jakarta.nojavaxpersistence.fixtures;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

/** MUST IGNORE: {@code javax.sql} is a JDK {@code javax} package, so a predicate widened to {@code javax..} fails. */
public class PersistenceSupport {

    private final DataSource dataSource;

    public PersistenceSupport(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean isReachable() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(1);
        }
    }
}
