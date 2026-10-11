package io.github.milczekt1.corral.rules.java.nooptionalmethodparameters.fixtures;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/** MUST IGNORE: two overloads in place of a parameter; Optional only as a constructor parameter, a field and a return type. */
public class PaymentQuery {

    private final Optional<Clock> clock;

    public PaymentQuery(Optional<Clock> clock) {
        this.clock = clock;
    }

    public List<String> search() {
        return List.of();
    }

    public List<String> search(String customerId) {
        return List.of(customerId);
    }

    public Optional<String> mostRecent() {
        return clock.map(Clock::toString);
    }

    public OptionalLong settledAt() {
        return OptionalLong.empty();
    }
}
