package io.github.milczekt1.corral.reflect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.fixtures.tree.AlphaFixtureRule;
import io.github.milczekt1.corral.fixtures.tree.BetaFixtureRule;
import io.github.milczekt1.corral.fixtures.tree.DecoyFieldsGroup;
import io.github.milczekt1.corral.fixtures.tree.FixtureLeafGroup;
import io.github.milczekt1.corral.fixtures.tree.FixtureRootGroup;
import io.github.milczekt1.corral.fixtures.tree.InheritingFixtureGroup;
import io.github.milczekt1.corral.fixtures.tree.RestrictedAccessGroup;
import io.github.milczekt1.corral.fixtures.tree.UninitialisedMemberGroup;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Exercises the walk against a fixture tree, so a change to a published rule cannot break it.
 *
 * <p>Field order is never asserted — neither {@code Class.getDeclaredFields()} nor supertype visit
 * order guarantees one.
 */
class PublishedRulesTest {

    private static Set<String> descriptionsOf(List<ArchRule> rules) {
        return rules.stream().map(ArchRule::getDescription).collect(Collectors.toSet());
    }

    @Test
    void descendsThroughNestedGroups() {
        List<ArchRule> found = PublishedRules.rulesReachableFrom(FixtureRootGroup.class);

        // Alpha twice (via the leaf group and directly), beta once — depth one would see one alpha.
        assertEquals(3, found.size(), "walk did not descend through the nested group");
        assertEquals(Set.of("test.no-alpha-fixture", "test.no-beta-fixture"), descriptionsOf(found));
    }

    @Test
    void idsOfIsDistinctEvenWhenARuleIsReachableTwice() {
        assertEquals(Set.of("test.no-alpha-fixture", "test.no-beta-fixture"),
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
        assertEquals(Set.of("test.no-alpha-fixture"), PublishedRules.idsOf(AlphaFixtureRule.class));
    }

    @Test
    void archRuleFieldsOfFindsMembersInheritedFromASuperclassAndFromAnInterface() {
        // InheritingFixtureGroup declares nothing itself; ArchUnit resolves members over supertypes.
        List<ArchRule> found = PublishedRules.archRuleFieldsOf(InheritingFixtureGroup.class);

        assertEquals(Set.of("fixture.inherited-from-superclass", "fixture.inherited-from-interface"),
                descriptionsOf(found));
    }

    @Test
    void archTestsFieldsOfFindsNestedGroupsInheritedFromASuperclass() {
        List<ArchTests> members = PublishedRules.archTestsFieldsOf(InheritingFixtureGroup.class);

        assertEquals(Set.of(BetaFixtureRule.class),
                members.stream().map(ArchTests::getDefinitionLocation).collect(Collectors.toSet()));
    }

    @Test
    void theWalkDescendsThroughAnInheritedNestedGroup() {
        // An inherited ArchTests member is descended into like any other.
        assertEquals(
                Set.of("fixture.inherited-from-superclass", "fixture.inherited-from-interface",
                        "test.no-beta-fixture"),
                PublishedRules.idsOf(InheritingFixtureGroup.class));
    }

    @Test
    void readsMembersThatAreNotPublicAndMembersOfClassesThatAreNotPublic() {
        // Both shapes are ones ArchUnit evaluates; the second is the consumer example's own.
        assertEquals(Set.of("fixture.restricted-field", "fixture.restricted-class"),
                PublishedRules.idsOf(RestrictedAccessGroup.class));
    }

    @Test
    void anUninitialisedMemberFailsWithTheFieldNamedAndTheHazardExplained() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> PublishedRules.archRuleFieldsOf(UninitialisedMemberGroup.class));

        assertTrue(thrown.getMessage().contains("declaredAboveWhatItReads"),
                "the failure must name the offending field, not surface as a bare NullPointerException"
                        + " from the collector: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains("Declare the @ArchTest field after the constants"),
                "the failure must point at the initialisation-order hazard that causes it: "
                        + thrown.getMessage());
    }
}
