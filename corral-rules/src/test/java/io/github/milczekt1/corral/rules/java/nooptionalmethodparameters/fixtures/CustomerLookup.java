package io.github.milczekt1.corral.rules.java.nooptionalmethodparameters.fixtures;

import java.util.List;
import java.util.Optional;

/** MUST FLAG {@code lookup}: an interface's methods are checked like a class's. */
public interface CustomerLookup {

    default List<String> lookup(Optional<String> id) {
        return id.map(List::of).orElse(List.of());
    }
}
