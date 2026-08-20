package io.github.milczekt1.llamaguard.fixtures.tree;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import lombok.experimental.UtilityClass;

/**
 * A group mixing a nested group with a rule class, so one root exercises both descent paths.
 *
 * <p>{@link AlphaFixtureRule} is reachable twice — once through {@link FixtureLeafGroup} and once
 * directly. That is deliberate: it is what makes the distinctness of {@code idsOf} testable, and it
 * mirrors the real catalog's supported case of one rule belonging to two groups.
 */
@UtilityClass
public class FixtureRootGroup {

    @ArchTest
    public static final ArchTests leafGroup = ArchTests.in(FixtureLeafGroup.class);

    @ArchTest
    public static final ArchTests alphaAgain = ArchTests.in(AlphaFixtureRule.class);
}
