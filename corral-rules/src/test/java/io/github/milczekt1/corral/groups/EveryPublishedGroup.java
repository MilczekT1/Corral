package io.github.milczekt1.corral.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import lombok.experimental.UtilityClass;

/**
 * Every group this catalog publishes, as one root the invariant tests here can walk.
 *
 * <p>Test-scoped on purpose. Corral publishes groups, not an all-in-one root: a consumer composes
 * the groups they want in their own module, so the set of rules that run is a decision their repo
 * records rather than one an upgrade can change. This class is the same shape a consumer builds,
 * but it exists only so {@code PublishedCatalogTest}, {@code RuleIdGrammarTest} and
 * {@code RuleRegistryCompletenessTest} have a definition of "everything published".
 *
 * <p>{@code PublishedCatalogTest.everyPublishedGroupIsReachableFromHere} fails when a group is added
 * to the catalog and not listed below, which is the only thing keeping this in step with
 * {@code src/main}.
 */
@UtilityClass
public class EveryPublishedGroup {

    @ArchTest
    public static final ArchTests testing = ArchTests.in(TestingRulesGroup.class);

    @ArchTest
    public static final ArchTests logging = ArchTests.in(LoggingRulesGroup.class);
}
