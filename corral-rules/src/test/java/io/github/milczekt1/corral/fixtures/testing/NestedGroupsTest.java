package io.github.milczekt1.corral.fixtures.testing;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * A conventionally named outer class whose real tests live in {@code @Nested} inner classes.
 *
 * <p>ArchUnit imports each inner class as its own {@code JavaClass} named {@code WhenEmpty} or
 * {@code WhenPopulated} — neither ends in Test or IT, and the naming rule must stay silent.
 */
public class NestedGroupsTest {

    @Nested
    class WhenEmpty {
        @Test
        void reportsNothing() {
        }
    }

    @Nested
    class WhenPopulated {
        @Test
        void reportsEverything() {
        }
    }
}
