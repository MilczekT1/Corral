package io.github.milczekt1.llamaguard.groups;

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
 */
@UtilityClass
public class AllCentralRules {

    @ArchTest
    public static final ArchTests testing = ArchTests.in(TestingRulesGroup.class);
}
