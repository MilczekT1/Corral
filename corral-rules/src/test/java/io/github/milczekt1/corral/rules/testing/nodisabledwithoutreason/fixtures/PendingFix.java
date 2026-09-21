package io.github.milczekt1.corral.rules.testing.nodisabledwithoutreason.fixtures;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.junit.jupiter.api.Disabled;

/** MUST FLAG here, at the declaration — a composed annotation hiding a bare disable. */
@Disabled
@Retention(RetentionPolicy.RUNTIME)
public @interface PendingFix {
}
