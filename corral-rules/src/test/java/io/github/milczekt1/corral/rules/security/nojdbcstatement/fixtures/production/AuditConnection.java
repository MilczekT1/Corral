package io.github.milczekt1.corral.rules.security.nojdbcstatement.fixtures.production;

import java.sql.Connection;

/**
 * SUPPORT: a driver-specific subinterface; a call through it has this, not {@code Connection}, as owner.
 */
public interface AuditConnection extends Connection {
}
