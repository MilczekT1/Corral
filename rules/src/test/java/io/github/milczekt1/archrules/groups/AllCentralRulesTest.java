package io.github.milczekt1.archrules.groups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.archrules.testsupport.PublishedRules;
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
 *   <li>{@link AllCentralRules#groups()} is what the completeness and README tooling reads.</li>
 * </ul>
 *
 * <p>Diverge them and the build stays green while nobody ever runs the new group's rules. These
 * tests make that divergence fail.
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

    @Test
    void everyGroupInGroupsIsAlsoExposedAsAnArchTestsField() {
        // A group present only in groups() is documented and completeness-checked but never
        // evaluated by any consumer — the worst possible failure mode: silent non-enforcement.
        assertEquals(Set.copyOf(AllCentralRules.groups()), classesExposedToConsumers(),
                "AllCentralRules.groups() and its @ArchTest ArchTests fields have diverged; every "
                        + "group needs BOTH an `@ArchTest public static final ArchTests` field and an "
                        + "entry in groups()");
    }

    @Test
    void thereIsExactlyOneArchTestsFieldPerGroup() {
        // Set equality above would tolerate two fields pointing at the same group.
        assertEquals(AllCentralRules.groups().size(),
                PublishedRules.archTestsFieldsOf(AllCentralRules.class).size(),
                "one @ArchTest ArchTests field per group, no duplicates");
    }

    @Test
    void everyExposedGroupContributesAtLeastOneRule() {
        for (Class<?> group : classesExposedToConsumers()) {
            assertFalse(PublishedRules.archRuleFieldsOf(group).isEmpty(),
                    group.getSimpleName() + " is aggregated but publishes no @ArchTest ArchRule field, "
                            + "so consumers evaluate an empty node");
        }
    }

    @Test
    void groupsAreListedInDocumentationOrder() {
        assertEquals(List.of(DatabaseRules.class, TestingRules.class), AllCentralRules.groups());
    }
}
