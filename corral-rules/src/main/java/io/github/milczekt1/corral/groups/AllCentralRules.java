package io.github.milczekt1.corral.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import lombok.experimental.UtilityClass;

/**
 * Opt into every central rule group with a single field:
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "com.acme", importOptions = ImportOption.DoNotIncludeJars.class)
 * class CentralArchitectureTest {
 *     @ArchTest
 *     static final ArchTests all = ArchTests.in(AllCentralRules.class);
 * }
 * }</pre>
 *
 * <p>An {@code @ArchTest ArchTests} field is the whole declaration: it is what
 * {@code ArchTests.in(...)} descends into, so a member without one is a member nobody evaluates.
 *
 * <p>A member may be another group or a rule class. See the README for the full growth path.
 *
 * <p>{@link ConfigurationChecksGroup} is a member like any other, but it is not an architecture rule
 * group: it holds the check that every line of the consumer's {@code corral-exclusions.txt} names a
 * rule that exists. See that class for why it is wired here rather than in {@code guard()}, and for
 * the deliberate mutual reference between it and this class.
 */
@UtilityClass
public class AllCentralRules {

    @ArchTest
    public static final ArchTests testing = ArchTests.in(TestingRulesGroup.class);

    @ArchTest
    public static final ArchTests logging = ArchTests.in(LoggingRulesGroup.class);

    @ArchTest
    public static final ArchTests configurationChecks = ArchTests.in(ConfigurationChecksGroup.class);
}
