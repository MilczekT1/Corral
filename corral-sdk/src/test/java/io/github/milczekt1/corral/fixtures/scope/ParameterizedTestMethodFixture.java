package io.github.milczekt1.corral.fixtures.scope;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A test class holding no {@code @Test} anywhere: {@code @ParameterizedTest} is itself annotated
 * {@code @TestTemplate} and is only reachable through a meta-annotation walk. A direct annotation
 * check would place this class outside test scope while JUnit runs it happily.
 *
 * <p>Never executed: Surefire excludes {@code **}{@code /fixtures/}{@code **} in this module's POM.
 */
public class ParameterizedTestMethodFixture {

    @ParameterizedTest
    @ValueSource(strings = {"only value"})
    void verifiesSomethingPerValue(String value) {
        // Deliberately empty: only the annotation on this method is under test.
    }
}
