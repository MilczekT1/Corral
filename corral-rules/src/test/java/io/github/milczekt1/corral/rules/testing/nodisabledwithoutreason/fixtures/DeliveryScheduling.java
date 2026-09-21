package io.github.milczekt1.corral.rules.testing.nodisabledwithoutreason.fixtures;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

/** MUST NOT FLAG: a stated reason on the type, and two conditional annotations on the methods. */
@Disabled("Blocked on the carrier sandbox, see #900 — re-enable when it returns")
public class DeliveryScheduling {

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void schedulesTheDelivery() {
    }

    @DisabledIfEnvironmentVariable(named = "CI", matches = "true")
    @Test
    void retriesTheDelivery() {
    }
}
