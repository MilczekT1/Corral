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
 * <p>A member may be another group or a rule class, and it is evaluated only through an
 * {@code @ArchTest ArchTests} field — that is what {@code ArchTests.in(...)} descends into.
 */
@UtilityClass
public class AllCentralRules {

    @ArchTest
    public static final ArchTests testing = ArchTests.in(TestingRulesGroup.class);

    @ArchTest
    public static final ArchTests logging = ArchTests.in(LoggingRulesGroup.class);
}
