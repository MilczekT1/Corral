package io.github.milczekt1.corral.groups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.milczekt1.corral.doc.DeprecatedRule;
import io.github.milczekt1.corral.reflect.PublishedRules;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Enforces the shape of every id Corral itself ships: {@code corral} at segment 1 and a polarity
 * marker on the slug. Checked against {@link EveryPublishedGroup} and {@link EveryStandaloneRule} — an
 * id is a freeze-store key whether or not a group names the rule. {@code RuleDoc} applies the hygiene
 * caps a consumer's own id needs.
 *
 * <p>A retired id (see {@link DeprecatedRule#retiredIds()}) is exempt from both checks.
 *
 * <p>Both read {@link DeprecatedRule#retiredIds()} only after {@link #shippedIds()} has run, which is
 * what forces every retirement to register. Do not hoist the call.
 */
class RuleIdGrammarTest {

    /** Every id Corral publishes starts with this vendor prefix — see CONTRIBUTING. */
    private static final String VENDOR_PREFIX = "corral";

    private static Set<String> shippedIds() {
        Set<String> ids = new LinkedHashSet<>(PublishedRules.idsOf(EveryPublishedGroup.class));
        ids.addAll(PublishedRules.idsOf(EveryStandaloneRule.class));
        return ids;
    }

    /** Guards the two tests below, whose loops would pass vacuously over an empty catalog. */
    @Test
    void bothRootsYieldIds() {
        assertFalse(PublishedRules.idsOf(EveryPublishedGroup.class).isEmpty(),
                "EveryPublishedGroup published no id at all — the other tests here iterate this same set"
                        + " and would pass vacuously if it were empty");
        assertFalse(PublishedRules.idsOf(EveryStandaloneRule.class).isEmpty(),
                "EveryStandaloneRule yielded no id at all, so no standalone rule's id is grammar-checked here");
    }

    @Test
    void everyShippedIdStartsWithTheVendorPrefix() {
        assertNamespaceGrammar(shippedIds());
    }

    @Test
    void everyShippedSlugCarriesAPolarityMarker() {
        assertPolarityGrammar(shippedIds());
    }

    /** Package-private so {@code DeprecatedRuleRetirementTest} runs this same check on a fixture. */
    static void assertNamespaceGrammar(Set<String> ids) {
        Set<String> retiredIds = DeprecatedRule.retiredIds();
        for (String id : ids) {
            if (retiredIds.contains(id)) {
                continue; // predates the grammar, including the vendor prefix; see DeprecatedRule.retiredIds().
            }
            String prefix = id.split("\\.")[0];

            assertEquals(VENDOR_PREFIX, prefix,
                    () -> "id '" + id + "' does not start with '" + VENDOR_PREFIX + "'. Every id"
                            + " Corral publishes carries the vendor prefix, so a consumer's own"
                            + " namespace can never collide with Corral's.");
        }
    }

    static void assertPolarityGrammar(Set<String> ids) {
        Set<String> retiredIds = DeprecatedRule.retiredIds();
        for (String id : ids) {
            if (retiredIds.contains(id)) {
                continue; // predates the grammar; see DeprecatedRule.retiredIds().
            }
            String[] segments = id.split("\\.");
            String slug = segments[segments.length - 1];
            boolean marked = slug.startsWith("no-") || slug.contains("-must-");

            assertTrue(marked,
                    () -> "id '" + id + "' has no polarity marker: a slug either starts with 'no-'"
                            + " (a prohibition) or contains '-must-' (an obligation, read as"
                            + " <subject>-must-<predicate>, e.g. fields-must-be-final). Every"
                            + " published id needs one, including a depth-2 corral.<slug> framework"
                            + " meta-check, were one ever published.");
        }
    }
}
