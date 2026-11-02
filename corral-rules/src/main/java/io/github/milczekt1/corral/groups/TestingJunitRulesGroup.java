package io.github.milczekt1.corral.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.corral.rules.testing.nodisabledwithoutreason.NoDisabledWithoutReasonRule;
import io.github.milczekt1.corral.rules.testing.nojunit4.NoJUnit4Rule;
import lombok.experimental.UtilityClass;

/**
 * Testing rules that only mean something to a JUnit suite. Included in {@code TestingRulesGroup};
 * wire this group on its own only when not wiring that one.
 */
@UtilityClass
public class TestingJunitRulesGroup {

    @ArchTest
    public static final ArchTests noDisabledWithoutReason =
            ArchTests.in(NoDisabledWithoutReasonRule.class);

    @ArchTest
    public static final ArchTests noJUnit4 =
            ArchTests.in(NoJUnit4Rule.class);
}
