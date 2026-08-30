package io.github.milczekt1.corral.groups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.corral.reflect.PublishedRules;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Pins what this catalog publishes: the groups, and the exact ids reachable through them.
 *
 * <p>Walks {@link EveryPublishedGroup}, the test-scoped root standing in for a consumer's own
 * composition — no such root ships, so this is the only place the catalog is enumerated whole.
 */
class PublishedCatalogTest {

    private static final String GROUPS_PACKAGE = "io.github.milczekt1.corral.groups";

    /**
     * {@code ArchTests.getDefinitionLocation()} is {@code @Internal}, and the only way to ask a field
     * which class it aggregates. Read-only, so its removal is a compile error here and nothing more.
     */
    private static Set<Class<?>> membersOf(Class<?> group) {
        return PublishedRules.archTestsFieldsOf(group).stream()
                .map(ArchTests::getDefinitionLocation)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<Class<?>> groupsReachableFromRoot() {
        Set<Class<?>> reachable = new LinkedHashSet<>();
        collectGroups(EveryPublishedGroup.class, reachable);
        return reachable;
    }

    private static void collectGroups(Class<?> group, Set<Class<?>> collected) {
        for (Class<?> member : membersOf(group)) {
            if (collected.add(member)) {
                collectGroups(member, collected);
            }
        }
    }

    @Test
    void everyExposedMemberContributesAtLeastOneRule() {
        for (Class<?> member : membersOf(EveryPublishedGroup.class)) {
            assertFalse(PublishedRules.rulesReachableFrom(member).isEmpty(),
                    member.getSimpleName() + " is aggregated but publishes no rule, so consumers "
                            + "evaluate an empty node");
        }
    }

    /**
     * A published group nobody lists is a group no test here ever sees: its rules skip the doc,
     * grammar and id-uniqueness checks while looking, from {@code src/main}, entirely wired up.
     *
     * <p>Imported rather than reflected over because a package cannot be listed from the JDK.
     * {@code DO_NOT_INCLUDE_TESTS} keeps {@link EveryPublishedGroup} itself out of the expectation.
     */
    @Test
    void everyPublishedGroupIsReachableFromHere() {
        Set<String> listed = groupsReachableFromRoot().stream()
                .map(Class::getName)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> published = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(GROUPS_PACKAGE).stream()
                .map(JavaClass::getName)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        assertFalse(published.isEmpty(),
                GROUPS_PACKAGE + " imported no class at all, so this check would pass vacuously");

        for (String group : published) {
            assertTrue(listed.contains(group),
                    group + " is published from " + GROUPS_PACKAGE + " but is not reachable from "
                            + EveryPublishedGroup.class.getSimpleName() + ", so nothing here checks"
                            + " its rules. Add an @ArchTest ArchTests field for it.");
        }
    }

    @Test
    void ruleDiscoveryDescendsThroughNestedGroups() {
        // A group whose members are themselves groups must still yield its rules.
        Set<String> ids = PublishedRules.idsOf(EveryPublishedGroup.class);

        assertEquals(Set.of(
                "corral.test.class-names-must-end-with-test-or-it",
                "corral.test.no-mocked-repository-in-integration-test",
                "corral.logging.no-system-out",
                "corral.logging.no-system-err"), ids);
    }
}
