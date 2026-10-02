package io.github.milczekt1.corral.rules.security.nojdbcstatement.fixtures;

import java.sql.Connection;

/** A driver-specific subinterface: a call through it has this, not {@code Connection}, as owner. */
public interface AuditConnection extends Connection {
}
