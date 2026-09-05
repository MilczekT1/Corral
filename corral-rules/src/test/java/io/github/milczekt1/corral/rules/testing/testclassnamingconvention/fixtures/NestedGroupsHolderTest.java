package io.github.milczekt1.corral.rules.testing.testclassnamingconvention.fixtures;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * MUST IGNORE, inner classes included: ArchUnit imports {@code WhenEmpty} and {@code WhenPopulated}
 * as their own {@code JavaClass}, and neither ends in an accepted suffix — but a {@code @Nested}
 * group runs through its enclosing class, whose name is the only one a build tool ever reads.
 */
public class NestedGroupsHolderTest {

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
