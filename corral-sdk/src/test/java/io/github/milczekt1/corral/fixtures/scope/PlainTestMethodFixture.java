package io.github.milczekt1.corral.fixtures.scope;

import org.junit.jupiter.api.Test;

/**
 * A directly annotated {@code @Test} method, on a class whose name ends in neither {@code Test} nor
 * {@code IT} — so no naming convention would recognise it. Never executed: Surefire excludes
 * {@code **}{@code /fixtures/}{@code **}.
 */
public class PlainTestMethodFixture {

    @Test
    void verifiesSomething() {
        // Deliberately empty: only the annotation on this method is under test.
    }
}
