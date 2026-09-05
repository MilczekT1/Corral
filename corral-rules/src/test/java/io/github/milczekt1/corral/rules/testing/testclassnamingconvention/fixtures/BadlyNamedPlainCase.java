package io.github.milczekt1.corral.rules.testing.testclassnamingconvention.fixtures;

import org.junit.jupiter.api.Test;

/**
 * MUST FLAG: a plain {@code @Test} in a top-level class whose name ends in none of the three
 * accepted suffixes.
 */
public class BadlyNamedPlainCase {

    @Test
    void notSelectedByNameConvention() {
    }
}
