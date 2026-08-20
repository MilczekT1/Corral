package io.github.milczekt1.llamaguard.reflect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.llamaguard.fixtures.tree.AlphaFixtureRule;
import io.github.milczekt1.llamaguard.fixtures.tree.DecoyFieldsGroup;
import io.github.milczekt1.llamaguard.fixtures.tree.FixtureLeafGroup;
import io.github.milczekt1.llamaguard.fixtures.tree.FixtureRootGroup;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Exercises the walk against a fixture tree rather than the real catalog. The SDK is a framework;
 * a change to a published rule must not be able to break a framework test.
 *
 * <p>Field order is never asserted: {@code Class.getDeclaredFields()} makes no ordering guarantee.
 */
class PublishedRulesTest {

    private static Set<String> descriptionsOf(List<ArchRule> rules) {
        return rules.stream().map(ArchRule::getDescription).collect(Collectors.toSet());
    }

    @Test
    void descendsThroughNestedGroups() {
        List<ArchRule> found = PublishedRules.rulesReachableFrom(FixtureRootGroup.class);

        // Alpha twice (via the leaf group and directly), beta once. A walk that stopped at depth one
        // would see only the direct alpha.
        assertEquals(3, found.size(), "walk did not descend through the nested group");
        assertEquals(Set.of("fixture.alpha", "fixture.beta"), descriptionsOf(found));
    }

    @Test
    void idsOfIsDistinctEvenWhenARuleIsReachableTwice() {
        assertEquals(Set.of("fixture.alpha", "fixture.beta"),
                PublishedRules.idsOf(FixtureRootGroup.class));
    }

    @Test
    void archRuleFieldsOfIgnoresUnannotatedAndNonStaticFields() {
        List<ArchRule> found = PublishedRules.archRuleFieldsOf(DecoyFieldsGroup.class);

        assertEquals(1, found.size(), "only the annotated static field is published");
        assertEquals(Set.of("fixture.decoy-published"), descriptionsOf(found));
    }

    @Test
    void archTestsFieldsOfReturnsOnlyGroupMembers() {
        List<ArchTests> members = PublishedRules.archTestsFieldsOf(FixtureRootGroup.class);

        assertEquals(2, members.size());
        assertEquals(Set.of(FixtureLeafGroup.class, AlphaFixtureRule.class),
                members.stream().map(ArchTests::getDefinitionLocation).collect(Collectors.toSet()));
    }

    @Test
    void aRuleClassIsAValidRootOfItsOwn() {
        // ArchTests.in(X) treats a rule class and a group identically, so the walk must too.
        assertEquals(Set.of("fixture.alpha"), PublishedRules.idsOf(AlphaFixtureRule.class));
    }
}
