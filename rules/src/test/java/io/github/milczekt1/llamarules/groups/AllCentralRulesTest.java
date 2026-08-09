package io.github.milczekt1.llamarules.groups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.llamarules.testsupport.PublishedRules;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * {@link AllCentralRules} lists every group twice and the two lists mean different things:
 *
 * <ul>
 *   <li>the {@code @ArchTest ArchTests} fields are the <em>only</em> members
 *       {@code ArchTests.in(AllCentralRules.class)} descends into — i.e. what consumers evaluate;</li>
 *   <li>{@link AllCentralRules#members()} is what the completeness and README tooling reads.</li>
 * </ul>
 *
 * <p>Diverge them and the build stays green while nobody ever runs the new group's rules.
 * {@link GroupMembershipTest} makes that divergence fail, here and at every level below. What is
 * left in this class is what is specific to the root: that every exposed member actually
 * contributes rules, and that the published id set is exactly the seeded one.
 */
class AllCentralRulesTest {

    /**
     * {@code ArchTests.getDefinitionLocation()} is annotated {@code @Internal}, so it is not part of
     * ArchUnit's public API. Reading it here is deliberate and confined to this test: it is the only
     * way to ask an {@code ArchTests} field which class it actually aggregates, and that question is
     * exactly what must not drift. It is read-only, so the worst case of ArchUnit removing it is a
     * compile error in this test — never a wrong verdict in a consumer's build.
     */
    private static Set<Class<?>> classesExposedToConsumers() {
        return PublishedRules.archTestsFieldsOf(AllCentralRules.class).stream()
                .map(ArchTests::getDefinitionLocation)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // members() vs @ArchTest ArchTests fields is asserted for AllCentralRules — and for every group
    // below it — by GroupMembershipTest, which walks the tree rather than naming groups one by one.

    @Test
    void everyExposedMemberContributesAtLeastOneRule() {
        for (Class<?> member : classesExposedToConsumers()) {
            assertFalse(PublishedRules.rulesReachableFrom(member).isEmpty(),
                    member.getSimpleName() + " is aggregated but publishes no rule, so consumers "
                            + "evaluate an empty node");
        }
    }

    @Test
    void groupsAreListedInDocumentationOrder() {
        assertEquals(List.of(TestingRules.class), AllCentralRules.members());
    }

    @Test
    void ruleDiscoveryDescendsThroughNestedGroups() {
        // A group whose members are themselves groups (or rule classes reached via ArchTests) must
        // still yield its rules. Before nesting support this returns empty for anything but a group
        // that declares @ArchTest ArchRule fields directly.
        Set<String> ids = PublishedRules.idSet();

        assertEquals(Set.of(
                "test.class-naming-convention",
                "test.no-mocked-repository-in-integration-test"), ids);
    }
}
