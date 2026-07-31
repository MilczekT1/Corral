package io.github.milczekt1.archrules.groups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.archrules.RuleDoc;
import io.github.milczekt1.archrules.RuleRegistry;
import io.github.milczekt1.archrules.testsupport.PublishedRules;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Guards the contract between the rules consumers evaluate and their documentation.
 *
 * <p>Everything here is keyed on {@link PublishedRules} rather than {@code RuleRegistry.all()}.
 * The registry is process-wide static state and Surefire reuses one JVM, so sibling tests that
 * register throwaway docs pollute it; asserting over its total contents would make these tests
 * depend on run order.
 */
class RuleRegistryCompletenessTest {

    @Test
    void everyPublishedRuleHasARegisteredDoc() {
        for (ArchRule rule : PublishedRules.all()) {
            String description = rule.getDescription();
            assertTrue(RuleRegistry.find(description).isPresent(),
                    "rule description '" + description + "' is not a registered RuleDoc id — the failure"
                            + " formatter would fall back to plain ArchUnit output for it");
        }
    }

    @Test
    void everyPublishedRuleHasUsableGuidance() {
        for (ArchRule rule : PublishedRules.all()) {
            String description = rule.getDescription();
            Optional<RuleDoc> found = RuleRegistry.find(description);

            assertTrue(found.isPresent(), "no doc registered for published rule '" + description + "'");
            RuleDoc doc = found.get();
            assertFalse(doc.why().isBlank(), doc.id() + " has a blank why");
            assertFalse(doc.howToFix().isBlank(), doc.id() + " has a blank howToFix");
        }
    }

    @Test
    void ruleIdsAreUnique() {
        List<String> ids = PublishedRules.ids();
        Set<String> unique = new LinkedHashSet<>(ids);

        assertEquals(ids.size(), unique.size(), "duplicate rule ids among published rules: " + ids);
    }

    // ---------------------------------------------------------------------------------------------
    // Evidence for the loadAll()/rulesReachableFrom recursion, using NestedFixtureGroup /
    // NestedFixtureLeaf — a throwaway two-level arrangement (group -> ArchTests -> rule class ->
    // ArchRule), the shape real rules will have after the rule-per-class move. Neither fixture is
    // reachable from AllCentralRules, wired to run under any @AnalyzeClasses test class, or
    // referenced by the published id set — they exist purely to prove the tooling generalises,
    // without moving or touching a real rule. They are separate top-level public classes (not
    // nested in this package-private test class) because reflective Field.get requires the
    // declaring class itself to be accessible from PublishedRules' package.

    @Test
    void loadAllAndRulesReachableFromBothDescendThroughNestedArchTestsFields() {
        // AllCentralRules.loadAll(List<Class<?>>) is package-private specifically so this test can
        // drive it directly against the fixture, independent of the real MEMBERS/TestingRules. A
        // flat, single-level Class.forName over just NestedFixtureGroup would NOT initialise
        // NestedFixtureLeaf: ArchTests.in(X.class) only stores the Class object, it never touches
        // X (see AllCentralRules.nestedMembersOf's Javadoc for the full evidence). This is
        // therefore a genuine test of loadAll()'s own explicit recursion.
        AllCentralRules.loadAll(List.of(NestedFixtureGroup.class));

        assertTrue(RuleRegistry.find(NestedFixtureLeaf.DOC.id()).isPresent(),
                "AllCentralRules.loadAll() must recurse into nested @ArchTest ArchTests fields, not "
                        + "just the members passed to it directly");

        // Same fixture, now proving PublishedRules' own recursive walk (Step 3) also reaches the
        // leaf rule and reports its id correctly.
        List<ArchRule> reached = PublishedRules.rulesReachableFrom(NestedFixtureGroup.class);
        assertEquals(List.of(NestedFixtureLeaf.DOC.id()),
                reached.stream().map(ArchRule::getDescription).toList());
    }
}
