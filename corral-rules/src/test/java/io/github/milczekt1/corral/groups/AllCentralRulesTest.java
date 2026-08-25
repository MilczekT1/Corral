package io.github.milczekt1.corral.groups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.corral.reflect.PublishedRules;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * The {@code @ArchTest ArchTests} fields on {@link AllCentralRules} are the only members
 * {@code ArchTests.in(AllCentralRules.class)} descends into, so they are exactly what consumers
 * evaluate. These tests pin what that walk yields: every exposed member contributes rules, and the
 * published id set is the seeded one.
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
    void everyExposedMemberContributesAtLeastOneRule() {
        for (Class<?> member : classesExposedToConsumers()) {
            assertFalse(PublishedRules.rulesReachableFrom(member).isEmpty(),
                    member.getSimpleName() + " is aggregated but publishes no rule, so consumers "
                            + "evaluate an empty node");
        }
    }

    @Test
    void ruleDiscoveryDescendsThroughNestedGroups() {
        // A group whose members are themselves groups (or rule classes reached via ArchTests) must
        // still yield its rules. Before nesting support this returns empty for anything but a group
        // that declares @ArchTest ArchRule fields directly.
        Set<String> ids = PublishedRules.idsOf(AllCentralRules.class);

        assertEquals(Set.of(
                "test.class-naming-convention",
                "test.no-mocked-repository-in-integration-test",
                "logging.no-system-out",
                "logging.no-system-err",
                "corral.exclusions-resolve"), ids);
    }
}
