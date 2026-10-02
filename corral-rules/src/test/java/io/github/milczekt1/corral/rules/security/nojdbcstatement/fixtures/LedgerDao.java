package io.github.milczekt1.corral.rules.security.nojdbcstatement.fixtures;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** MUST FLAG: the call's owner is {@link AuditConnection}, so only assignability matches it. */
public class LedgerDao {

    public boolean entries(AuditConnection connection, String account) throws SQLException {
        Statement statement = connection.createStatement();
        try (ResultSet rows = statement.executeQuery("SELECT * FROM ledger WHERE account = '" + account + "'")) {
            return rows.next();
        }
    }
}
