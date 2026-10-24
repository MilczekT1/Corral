package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import java.time.Clock;
import java.time.Instant;
import mockit.MockUp;

/** MUST FLAG {@code mockit..}: a JMockit fake left in a production debug path. */
public class ClockOverride {

    Object freezeAt(Instant instant) {
        return new MockUp<Clock>() {
            @Override
            public String toString() {
                return "frozen at " + instant;
            }
        };
    }
}
