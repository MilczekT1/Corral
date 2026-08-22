package io.github.milczekt1.corral.fixtures.tree;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import lombok.experimental.UtilityClass;

/** A group whose members are rule classes — the leaf shape. */
@UtilityClass
public class FixtureLeafGroup {

    @ArchTest
    public static final ArchTests alpha = ArchTests.in(AlphaFixtureRule.class);

    @ArchTest
    public static final ArchTests beta = ArchTests.in(BetaFixtureRule.class);
}
