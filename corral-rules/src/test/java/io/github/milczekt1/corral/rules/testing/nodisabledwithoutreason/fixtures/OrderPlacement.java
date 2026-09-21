package io.github.milczekt1.corral.rules.testing.nodisabledwithoutreason.fixtures;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * MUST FLAG on {@code placesTheOrder} only. The enabled method and the explained one are the
 * must-not-match half: an over-broad predicate finds those too.
 */
public class OrderPlacement {

    @Disabled
    @Test
    void placesTheOrder() {
    }

    @Test
    void reservesStock() {
    }

    @Disabled("Blocked on gateway sandbox outage, see #412 — re-enable when it closes")
    @Test
    void refundsTheOrder() {
    }
}
