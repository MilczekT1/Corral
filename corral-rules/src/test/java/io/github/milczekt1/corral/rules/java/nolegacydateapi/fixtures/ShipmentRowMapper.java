package io.github.milczekt1.corral.rules.java.nolegacydateapi.fixtures;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/** MUST IGNORE: reads a {@code TIMESTAMP} column without {@code java.sql.Timestamp}. */
public class ShipmentRowMapper {

    public LocalDateTime shippedAt(ResultSet rs) throws SQLException {
        return rs.getObject("shipped_at", LocalDateTime.class);
    }
}
