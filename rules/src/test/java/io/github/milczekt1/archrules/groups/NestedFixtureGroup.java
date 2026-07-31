package io.github.milczekt1.archrules.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

/**
 * Fixture-only group for {@link RuleRegistryCompletenessTest}: points at {@link NestedFixtureLeaf}
 * the same way a real group will point at a rule class once rules move one level deeper. See
 * {@link NestedFixtureLeaf}'s Javadoc for why this exists and why it must be a top-level public
 * class.
 */
public final class NestedFixtureGroup {

    @ArchTest
    public static final ArchTests nested = ArchTests.in(NestedFixtureLeaf.class);

    private NestedFixtureGroup() {
    }
}
