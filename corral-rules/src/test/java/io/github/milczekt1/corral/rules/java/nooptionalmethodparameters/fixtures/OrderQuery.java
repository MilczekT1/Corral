package io.github.milczekt1.corral.rules.java.nooptionalmethodparameters.fixtures;

import java.util.List;
import java.util.Optional;

/** MUST FLAG {@code find}; MUST IGNORE {@code findByStatus}, which takes a plain type, and {@code latest}, which returns {@code Optional}. */
public class OrderQuery {

    public List<String> find(Optional<String> customerId) {
        return customerId.map(List::of).orElse(List.of());
    }

    public List<String> findByStatus(String status) {
        return List.of(status);
    }

    public Optional<String> latest() {
        return Optional.empty();
    }
}
