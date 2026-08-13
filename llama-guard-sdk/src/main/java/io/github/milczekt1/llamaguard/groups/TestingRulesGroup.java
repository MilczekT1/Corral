package io.github.milczekt1.llamaguard.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.llamaguard.rules.testing.NoMockedRepositoryInIntegrationTestRule;
import io.github.milczekt1.llamaguard.rules.testing.TestClassNamingConventionRule;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TestingRulesGroup {

    @ArchTest
    public static final ArchTests noMockedRepositoryInIntegrationTest =
            ArchTests.in(NoMockedRepositoryInIntegrationTestRule.class);

    @ArchTest
    public static final ArchTests testClassNamingConvention =
            ArchTests.in(TestClassNamingConventionRule.class);
}
