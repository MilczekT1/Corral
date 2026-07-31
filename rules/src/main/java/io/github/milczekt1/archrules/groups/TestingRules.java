package io.github.milczekt1.archrules.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.archrules.rules.testing.NoMockedRepositoryInIntegrationTest;
import io.github.milczekt1.archrules.rules.testing.TestClassNamingConvention;
import java.util.List;

/**
 * Rules about test hygiene.
 *
 * <p>These rules inspect <em>test</em> classes, which is why consumers must not configure
 * {@code ImportOption.DoNotIncludeTests} — doing so makes them pass vacuously.
 */
public final class TestingRules {

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

    private TestingRules() {
    }
}
