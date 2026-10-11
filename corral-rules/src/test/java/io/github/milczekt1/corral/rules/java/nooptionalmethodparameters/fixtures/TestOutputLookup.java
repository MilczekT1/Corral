package io.github.milczekt1.corral.rules.java.nooptionalmethodparameters.fixtures;

import java.util.List;
import java.util.Optional;

/**
 * MUST IGNORE when imported from where it compiles, test output. MUST FLAG if copied into a
 * production layout, which is how the test shows the scope clause, not the type, spares it.
 */
public class TestOutputLookup {

    public List<String> apply(Optional<String> code) {
        return code.map(List::of).orElse(List.of());
    }
}
