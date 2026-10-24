package io.github.milczekt1.corral.rules.exception.nogenericexceptionthrown.fixtures;

import java.util.Optional;

/**
 * MUST FLAG {@code new RuntimeException}, a {@code RuntimeException::new} reference, and an anonymous
 * {@code RuntimeException} on its constructor. MUST IGNORE a method called on a
 * {@code RuntimeException} and {@code new IllegalArgumentException}.
 */
public class OrderService {

    public String byId(long id) {
        throw new RuntimeException("order " + id + " not found");
    }

    public String firstLine(Optional<String> order) {
        return order.orElseThrow(RuntimeException::new);
    }

    public String byReference(String reference) {
        throw new RuntimeException("order " + reference + " not found") {
        };
    }

    public String reasonOf(RuntimeException failure) {
        return failure.getMessage();
    }

    public void validate(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
    }
}
