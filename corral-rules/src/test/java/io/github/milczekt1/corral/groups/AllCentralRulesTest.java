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
 * Pins what {@code ArchTests.in(AllCentralRules.class)} yields — exactly what consumers evaluate:
 * every exposed member contributes rules, and the published id set is the seeded one.
 */
class AllCentralRulesTest {

    /**
     * {@code ArchTests.getDefinitionLocation()} is {@code @Internal}, and the only way to ask a field
     * which class it aggregates. Read-only, so its removal is a compile error here and nothing more.
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
        // A group whose members are themselves groups must still yield its rules.
        Set<String> ids = PublishedRules.idsOf(AllCentralRules.class);

        assertEquals(Set.of(
                "corral.test.class-names-must-end-with-test-or-it",
                "corral.test.no-mocked-repository-in-integration-test",
                "corral.logging.no-system-out",
                "corral.logging.no-system-err"), ids);
    }
}
