package io.github.milczekt1.corral.rules.java.nooptionalfields.fixtures;

import java.time.Clock;
import java.util.Optional;

/** MUST FLAG: a bean built by a {@code @Bean} method carries no stereotype, so nothing marks it as one. */
public class FraudClient {

    private final Optional<Clock> fallbackClock;

    public FraudClient(Optional<Clock> fallbackClock) {
        this.fallbackClock = fallbackClock;
    }

    public boolean hasFallbackClock() {
        return fallbackClock.isPresent();
    }
}
