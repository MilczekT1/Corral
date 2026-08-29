package io.github.milczekt1.corral.fixtures.tree;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import lombok.experimental.UtilityClass;

/**
 * A group mixing a nested group with a rule class, so one root exercises both descent paths.
 *
 * <p>{@link AlphaFixtureRule} is reachable twice — once through {@link FixtureLeafGroup} and once
 * directly — which is what makes the distinctness of {@code idsOf} testable.
 */
@UtilityClass
public class FixtureRootGroup {

    @ArchTest
    public static final ArchTests leafGroup = ArchTests.in(FixtureLeafGroup.class);

    @ArchTest
    public static final ArchTests alphaAgain = ArchTests.in(AlphaFixtureRule.class);
}
