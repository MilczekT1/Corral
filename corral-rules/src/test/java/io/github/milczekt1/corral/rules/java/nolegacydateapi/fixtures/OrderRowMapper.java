package io.github.milczekt1.corral.rules.java.nolegacydateapi.fixtures;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

/** MUST FLAG the call on {@code java.sql.Timestamp}, a {@code java.util.Date} subclass; reading the id is fine. */
public class OrderRowMapper {

    public Instant createdAt(ResultSet rs) throws SQLException {
        rs.getString("id");
        return rs.getTimestamp("created_at").toInstant();
    }
}
