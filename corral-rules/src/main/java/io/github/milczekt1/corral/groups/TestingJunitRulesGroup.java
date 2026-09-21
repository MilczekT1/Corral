package io.github.milczekt1.corral.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.corral.rules.testing.nodisabledwithoutreason.NoDisabledWithoutReasonRule;
import io.github.milczekt1.corral.rules.testing.nojunit4.NoJUnit4Rule;
import lombok.experimental.UtilityClass;

/**
 * Testing rules that only mean something to a JUnit suite; {@code TestingRulesGroup} holds the ones
 * that hold for a TestNG or Spock suite just as well.
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
