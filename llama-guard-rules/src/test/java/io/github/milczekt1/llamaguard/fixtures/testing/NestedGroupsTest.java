package io.github.milczekt1.llamaguard.fixtures.testing;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * A conventionally named outer class whose real tests live in {@code @Nested} inner classes.
 *
 * <p>ArchUnit imports each inner class as its own {@code JavaClass} with the inner simple name
 * ({@code WhenEmpty}, {@code WhenPopulated}) — names ending in neither Test nor IT. The naming rule
 * must stay silent about them: Surefire selects only the enclosing class, so renaming a nested
 * group would change nothing.
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
