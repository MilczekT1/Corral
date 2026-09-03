package io.github.milczekt1.corral.rules.testing.testclassnamingconvention.fixtures;

import org.junit.jupiter.api.Test;

/** MUST FLAG: a plain {@code @Test} in a top-level class Surefire's convention never selects. */
public class BadlyNamedTestCase {

    @Test
    void surefireWillNeverRunMe() {
    }
}
