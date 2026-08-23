package io.github.milczekt1.corral.fixtures.scope;

import org.junit.jupiter.api.Test;

/**
 * A class whose only claim to being a test is a directly annotated {@code @Test} method — its name
 * ends in neither {@code Test} nor {@code IT}, so no naming convention would recognise it.
 *
 * <p>Never executed: Surefire excludes {@code **}{@code /fixtures/}{@code **} in this module's POM.
 */
public class PlainTestMethodFixture {

    @Test
    void verifiesSomething() {
        // Deliberately empty: only the annotation on this method is under test.
    }
}
