package io.github.milczekt1.corral.rules.java.nooptionalfields.fixtures;

import java.util.Optional;

/**
 * MUST IGNORE when imported from where it compiles, test output. MUST FLAG if copied into a
 * production layout, which is how the test shows the scope clause, not the type, spares it.
 */
public class TestOutputQuery {

    private Optional<String> keyword = Optional.empty();

    public String keywordOrBlank() {
        return keyword.orElse("");
    }
}
