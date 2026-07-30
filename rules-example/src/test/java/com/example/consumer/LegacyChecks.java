package com.example.consumer;

import org.junit.jupiter.api.Test;

/**
 * Deliberate, permanent violation of {@code test.class-naming-convention}.
 *
 * <p>It holds a JUnit test method but its name ends in neither {@code Test} nor {@code IT}, so no
 * build tool's default selection will ever run it — the exact failure that rule exists to catch.
 *
 * <p>It is left in place on purpose and frozen into the committed store, demonstrating the
 * library's central promise: adopting the rules records existing debt instead of blocking the
 * build. Only <em>new</em> violations fail.
 */
class LegacyChecks {

    @Test
    void checksSomethingNobodyRuns() {
        // Intentionally empty: this method's existence is the violation, not its body.
    }
}
