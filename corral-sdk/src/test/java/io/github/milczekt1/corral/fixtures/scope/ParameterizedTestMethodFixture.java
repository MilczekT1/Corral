package io.github.milczekt1.corral.fixtures.scope;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Holds no {@code @Test} anywhere — {@code @ParameterizedTest} is reachable only through the
 * {@code @TestTemplate} meta-annotation, so a direct annotation check would miss it while JUnit
 * runs it happily. Never executed: Surefire excludes {@code **}{@code /fixtures/}{@code **}.
 */
public class ParameterizedTestMethodFixture {

    @ParameterizedTest
    @ValueSource(strings = {"only value"})
    void verifiesSomethingPerValue(String value) {
        // Deliberately empty: only the annotation on this method is under test.
    }
}
