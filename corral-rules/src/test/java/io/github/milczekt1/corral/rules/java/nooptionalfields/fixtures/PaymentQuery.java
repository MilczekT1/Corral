package io.github.milczekt1.corral.rules.java.nooptionalfields.fixtures;

import java.util.Optional;
import java.util.OptionalLong;

/** MUST IGNORE: Optional only as a return type and a parameter type; the field is nullable. */
public class PaymentQuery {

    private String currency;

    private Long settledAt;

    public Optional<String> currency() {
        return Optional.ofNullable(currency);
    }

    public OptionalLong settledAt() {
        return settledAt == null ? OptionalLong.empty() : OptionalLong.of(settledAt);
    }

    public void filterBy(Optional<String> wanted) {
        currency = wanted.orElse(null);
    }
}
