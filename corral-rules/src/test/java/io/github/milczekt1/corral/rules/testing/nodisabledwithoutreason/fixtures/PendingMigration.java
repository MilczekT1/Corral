package io.github.milczekt1.corral.rules.testing.nodisabledwithoutreason.fixtures;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.junit.jupiter.api.Disabled;

/** MUST NOT FLAG: the same composition, with the reason written once for every use. */
@Disabled("Pending the 2026 pricing migration, see #900")
@Retention(RetentionPolicy.RUNTIME)
public @interface PendingMigration {
}
