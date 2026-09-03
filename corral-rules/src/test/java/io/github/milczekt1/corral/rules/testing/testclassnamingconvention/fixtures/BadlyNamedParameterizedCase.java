package io.github.milczekt1.corral.rules.testing.testclassnamingconvention.fixtures;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * MUST FLAG: holds no {@code @Test} at all, only {@code @ParameterizedTest}, which JUnit reaches
 * through {@code @TestTemplate}. Exactly as unexecutable as {@link BadlyNamedTestCase}.
 */
public class BadlyNamedParameterizedCase {

    @ParameterizedTest
    @ValueSource(strings = {"a", "b"})
    void surefireWillNeverRunMeEither(String value) {
    }
}
