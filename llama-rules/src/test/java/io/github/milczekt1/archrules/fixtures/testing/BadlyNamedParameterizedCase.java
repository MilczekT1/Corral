package io.github.milczekt1.archrules.fixtures.testing;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Holds no {@code @Test} method at all — only {@code @ParameterizedTest}. Exactly as unexecutable
 * as {@link BadlyNamedTestCase}, so the naming rule must flag it too.
 */
public class BadlyNamedParameterizedCase {

    @ParameterizedTest
    @ValueSource(strings = {"a", "b"})
    void surefireWillNeverRunMeEither(String value) {
    }
}
