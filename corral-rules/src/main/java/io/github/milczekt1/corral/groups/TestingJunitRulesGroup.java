package io.github.milczekt1.corral.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.corral.rules.testing.nodisabledwithoutreason.NoDisabledWithoutReasonRule;
import io.github.milczekt1.corral.rules.testing.nojunit4.NoJUnit4Rule;
import lombok.experimental.UtilityClass;

/**
 * Testing rules that only mean something to a JUnit suite.
 *
 * <p>Split from {@code TestingRulesGroup}, which holds the rules that hold for a TestNG or Spock
 * suite just as well: keeping both in one node means a non-JUnit consumer either takes the JUnit
 * rules or loses the framework-agnostic ones.
 *
 * <p>Group membership is not part of a rule id, so nothing here changed a freeze-store key — but a
 * consumer wiring only {@code TestingRulesGroup} stops running {@code corral.test.no-junit4} and
 * nothing tells them. Wire this group alongside it.
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
