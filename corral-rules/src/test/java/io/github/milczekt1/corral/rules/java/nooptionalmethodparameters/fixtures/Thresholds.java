package io.github.milczekt1.corral.rules.java.nooptionalmethodparameters.fixtures;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/** MUST FLAG each primitive variant, one method apiece. */
public class Thresholds {

    private int retries;

    private long timeoutMillis;

    private double ratio;

    public void withRetries(OptionalInt value) {
        retries = value.orElse(3);
    }

    public void withTimeout(OptionalLong value) {
        timeoutMillis = value.orElse(1_000L);
    }

    public void withRatio(OptionalDouble value) {
        ratio = value.orElse(0.5);
    }

    public String summary() {
        return retries + " " + timeoutMillis + " " + ratio;
    }
}
