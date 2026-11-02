package io.github.milczekt1.corral.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.corral.rules.testing.nomockedrepositoryinintegrationtest.NoMockedRepositoryInIntegrationTestRule;
import io.github.milczekt1.corral.rules.testing.nothreadsleep.NoThreadSleepRule;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TestingRulesGroup {

    @ArchTest
    public static final ArchTests noMockedRepositoryInIntegrationTest =
            ArchTests.in(NoMockedRepositoryInIntegrationTestRule.class);

    @ArchTest
    public static final ArchTests noThreadSleep =
            ArchTests.in(NoThreadSleepRule.class);

    @ArchTest
    public static final ArchTests junit =
            ArchTests.in(TestingJunitRulesGroup.class);

    @ArchTest
    public static final ArchTests mockito =
            ArchTests.in(TestingMockitoRulesGroup.class);
}
