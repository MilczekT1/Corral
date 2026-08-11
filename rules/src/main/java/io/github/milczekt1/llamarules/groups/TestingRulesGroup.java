package io.github.milczekt1.llamarules.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.llamarules.rules.testing.NoMockedRepositoryInIntegrationTestRule;
import io.github.milczekt1.llamarules.rules.testing.TestClassNamingConventionRule;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TestingRulesGroup {

    private static final List<Class<?>> MEMBERS = List.of(
            NoMockedRepositoryInIntegrationTestRule.class,
            TestClassNamingConventionRule.class);

    @ArchTest
    public static final ArchTests noMockedRepositoryInIntegrationTest =
            ArchTests.in(NoMockedRepositoryInIntegrationTestRule.class);

    @ArchTest
    public static final ArchTests testClassNamingConvention =
            ArchTests.in(TestClassNamingConventionRule.class);

    public static List<Class<?>> members() {
        return MEMBERS;
    }

}
