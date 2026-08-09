package io.github.milczekt1.llamarules.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.llamarules.rules.testing.NoMockedRepositoryInIntegrationTest;
import io.github.milczekt1.llamarules.rules.testing.TestClassNamingConvention;
import java.util.List;

public final class TestingRulesGroup {

    private static final List<Class<?>> MEMBERS = List.of(
            NoMockedRepositoryInIntegrationTest.class,
            TestClassNamingConvention.class);

    @ArchTest
    public static final ArchTests noMockedRepositoryInIntegrationTest =
            ArchTests.in(NoMockedRepositoryInIntegrationTest.class);

    @ArchTest
    public static final ArchTests testClassNamingConvention =
            ArchTests.in(TestClassNamingConvention.class);

    /** Members may be rule classes or nested groups. @see AllCentralRules#members() */
    public static List<Class<?>> members() {
        return MEMBERS;
    }

    private TestingRulesGroup() {
    }
}
