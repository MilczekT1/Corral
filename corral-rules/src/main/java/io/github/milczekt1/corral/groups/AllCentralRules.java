package io.github.milczekt1.corral.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.exclude.RuleExclusions;
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
 * <p>{@link #exclusionsResolve} rides along: it is not an architecture rule, it is the check that
 * every line of {@code corral-exclusions.txt} names a rule that exists. It lives here rather than in
 * {@code guard()} because only a walk from this root sees the whole catalog — mid-run, a rule being
 * evaluated can only see the rules loaded before it.
 */
@UtilityClass
public class AllCentralRules {

    @ArchTest
    public static final ArchTests testing = ArchTests.in(TestingRulesGroup.class);

    @ArchTest
    public static final ArchTests logging = ArchTests.in(LoggingRulesGroup.class);

    /**
     * Fails when {@code corral-exclusions.txt} names an id nothing here publishes and nothing has
     * registered — a typo, or a rule renamed upstream. Either way the line removes nothing while
     * reading in the diff as though it did.
     *
     * <p>Declared last: it walks this class's own {@code @ArchTest} fields, which must already be
     * initialised. The walk happens on evaluation, long after class initialisation, so the ordering
     * is belt and braces rather than load-bearing.
     */
    @ArchTest
    public static final ArchRule exclusionsResolve = RuleExclusions.resolvedAgainst(AllCentralRules.class);
}
