package io.github.milczekt1.corral.rules.security.nojdbcstatement.fixtures;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** MUST FLAG: the two-argument overload is still a {@code createStatement} call. */
public class ReportExporter {

    public boolean export(Connection connection, String region) throws SQLException {
        Statement statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        try (ResultSet rows = statement.executeQuery("SELECT * FROM reports WHERE region = '" + region + "'")) {
            return rows.next();
        }
    }
}
