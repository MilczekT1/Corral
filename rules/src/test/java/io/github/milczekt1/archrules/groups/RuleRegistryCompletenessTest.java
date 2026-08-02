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

    /**
     * The leaf's id as a <em>string literal</em>, deliberately not {@code NestedFixtureLeaf.DOC.id()}.
     *
     * <p>{@code DOC} is a {@code static final RuleDoc}, not a compile-time constant, so reading it
     * is itself a JLS class-initialisation trigger — and {@code NestedFixtureLeaf}'s static
     * initialiser is what registers the doc. Naming the field inside the assertion would therefore
     * satisfy the assertion by evaluating it: the test would pass with {@code loadAll} gutted to
     * {@code {}}. Keeping the id as text means nothing in this test touches the leaf class before
     * {@code loadAll} is asked to reach it.
     *
     * <p>Must stay in step with {@code NestedFixtureLeaf.DOC}'s id; the last assertion of the test
     * below pins that, once the leaf may safely be touched.
     */
    private static final String NESTED_FIXTURE_LEAF_ID = "test.nested-fixture-registry-propagation-check";

    @Test
    void loadAllAndRulesReachableFromBothDescendThroughNestedArchTestsFields() {
        // AllCentralRules.loadAll(List<Class<?>>) is package-private specifically so this test can
        // drive it directly against the fixture, independent of the real MEMBERS/TestingRules. A
        // flat, single-level Class.forName over just NestedFixtureGroup would NOT initialise
        // NestedFixtureLeaf: ArchTests.in(X.class) only stores the Class object, it never touches
        // X (see AllCentralRules.nestedMembersOf's Javadoc for the full evidence). This is
        // therefore a genuine test of loadAll()'s own explicit recursion.

        // Without this precondition the test cannot tell "loadAll recursed" from "something else
        // already initialised the leaf". The id is fixture-only and appears nowhere else in the
        // repository, and no other test walks NestedFixtureGroup, so nothing in the shared Surefire
        // JVM can register it first.
        assertFalse(RuleRegistry.find(NESTED_FIXTURE_LEAF_ID).isPresent(),
                "precondition: the leaf must not be registered before loadAll() is called — if it is, "
                        + "something initialised NestedFixtureLeaf and this test proves nothing");

        AllCentralRules.loadAll(List.of(NestedFixtureGroup.class));

        assertTrue(RuleRegistry.find(NESTED_FIXTURE_LEAF_ID).isPresent(),
                "AllCentralRules.loadAll() must recurse into nested @ArchTest ArchTests fields, not "
                        + "just the members passed to it directly");

        // Same fixture, now proving PublishedRules' own recursive walk (Step 3) also reaches the
        // leaf rule and reports its id correctly.
        List<ArchRule> reached = PublishedRules.rulesReachableFrom(NestedFixtureGroup.class);
        assertEquals(List.of(NESTED_FIXTURE_LEAF_ID),
                reached.stream().map(ArchRule::getDescription).toList());

        // Only now, with everything above already asserted, is it safe to touch the fixture class:
        // this pins the literal to the fixture so renaming the id fails here rather than quietly
        // turning the recursion check vacuous. It must stay last — and it must not become its own
        // @Test method, because JUnit's method order is not guaranteed and initialising the leaf
        // first would break the precondition above.
        assertEquals(NESTED_FIXTURE_LEAF_ID, NestedFixtureLeaf.DOC.id(),
                "the literal above must name the fixture leaf's actual id");
    }
}
