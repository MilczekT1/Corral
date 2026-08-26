package io.github.milczekt1.corral.groups;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.milczekt1.corral.doc.DeprecatedRule;
import io.github.milczekt1.corral.reflect.PublishedRules;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Enforces the shape of every id Corral itself publishes.
 *
 * <p>{@code RuleDoc}'s own constructor checks only universal hygiene — shape, length, depth — because
 * it is public SDK surface: a consumer's own {@code DocumentedRule} builds a {@code RuleDoc} against
 * it too, with a namespace of their own choosing. What a Corral id's vendor prefix and concern may be,
 * and what marks a slug as a prohibition or an obligation, is catalog taxonomy for THIS project's
 * rules, so it belongs here — checked against {@link AllCentralRules}, the whole published catalog,
 * and nowhere a consumer's own id would ever trip it.
 *
 * <p>Every id Corral publishes starts with the {@code corral} vendor prefix at segment 1 — see
 * CONTRIBUTING — so a consumer choosing their own namespace can never collide with Corral's. Segment 2
 * is either a closed concern vocabulary (a catalog rule, {@code corral.<concern>.<slug>}) or, for the
 * sole framework meta-check {@code corral.exclusions-resolve}, the slug itself
 * ({@code corral.<slug>}).
 *
 * <p>An id is the freeze-store key: it cannot change once a consumer has frozen it. The vocabulary
 * below is closed on purpose, so that "which bucket" never becomes an editorial argument —
 * relitigating it is exactly what produces a rename, and a rename silently stops enforcement in every
 * consumer that froze the old id.
 *
 * <p>A retired id (see {@link DeprecatedRule#retiredIds()}) is exempt from the namespace and polarity
 * checks below — it predates the grammar, including the vendor prefix, and can never be renamed to
 * comply, that being the entire point of retiring rather than renaming. It still has to pass the
 * qualifier-segment check: a retired id is still a freeze-store file name.
 *
 * <p>The namespace and polarity checks below call {@link DeprecatedRule#retiredIds()} <em>after</em>
 * {@link #publishedIds()} has already run. That ordering is not a race to "fix" by reading
 * {@code retiredIds()} earlier: {@code publishedIds()} reads {@link AllCentralRules}'s
 * {@code @ArchTest} fields, which forces every member class — including one holding a
 * {@code DeprecatedRule.supersededBy(...)} call in a field initialiser — to run its static
 * initialiser first. By the time this method returns, every retirement it could possibly report has
 * already registered.
 */
class RuleIdGrammarTest {

    /** Every id Corral publishes starts with this vendor prefix — see CONTRIBUTING. */
    private static final String VENDOR_PREFIX = "corral";

    /** Segment-2 values, closed on purpose — see CONTRIBUTING. {@code java<N>} is matched separately. */
    private static final Set<String> CONCERNS = Set.of(
            "api", "concurrency", "exception", "jackson", "jakarta", "java",
            "layering", "logging", "lombok", "naming", "security", "spring", "test");

    /** A JDK-version concern, for a rule about an API that exists only from that release. */
    private static final Pattern JAVA_VERSION = Pattern.compile("^java[0-9]++$");

    /** Segment-3 values allowed to precede the slug: a library qualifier. */
    private static final Set<String> QUALIFIERS = Set.of("mockito", "powermock", "junit");

    private static Set<String> publishedIds() {
        return PublishedRules.idsOf(AllCentralRules.class);
    }

    /**
     * Guards the three tests below against the vacuous pass a wholly empty catalog would otherwise
     * produce: a bare {@code for} loop over an empty set asserts nothing and reports green. This does
     * not replace {@code PublishedRuleIdsTest} — it means this class does not depend on that other
     * test existing to catch the same failure.
     */
    @Test
    void publishedIdsIsNotEmpty() {
        assertFalse(publishedIds().isEmpty(),
                "AllCentralRules published no id at all — the other tests here iterate this same set"
                        + " and would pass vacuously if it were empty");
    }

    @Test
    void everyPublishedIdStartsWithTheVendorPrefixAndAClosedConcern() {
        assertNamespaceGrammar(publishedIds());
    }

    @Test
    void everyPublishedSlugCarriesAPolarityMarkerExceptCorralsOwnMetaChecks() {
        assertPolarityGrammar(publishedIds());
    }

    @Test
    void aFourthSegmentIsOnlyALibraryQualifier() {
        assertQualifierSegmentGrammar(publishedIds());
    }

    /**
     * Package-private so {@code DeprecatedRuleRetirementTest} can run the very check this class runs
     * against a fixture retirement, instead of re-implementing the rule and risking the two drifting
     * apart.
     */
    static void assertNamespaceGrammar(Set<String> ids) {
        Set<String> retiredIds = DeprecatedRule.retiredIds();
        for (String id : ids) {
            if (retiredIds.contains(id)) {
                continue; // predates the grammar, including the vendor prefix; see DeprecatedRule.retiredIds().
            }
            String[] segments = id.split("\\.");
            String prefix = segments[0];

            assertTrue(VENDOR_PREFIX.equals(prefix),
                    () -> "id '" + id + "' does not start with '" + VENDOR_PREFIX + "'. Every id"
                            + " Corral publishes carries the vendor prefix, so a consumer's own"
                            + " namespace can never collide with Corral's.");

            if (segments.length >= 3) {
                String concern = segments[1];

                assertTrue(CONCERNS.contains(concern) || JAVA_VERSION.matcher(concern).matches(),
                        () -> "id '" + id + "' has concern '" + concern + "' at segment 2, which is"
                                + " not a Corral concern. Use one of " + CONCERNS + ", or java<N> for a"
                                + " rule about an API introduced in that JDK. The list is closed so"
                                + " that 'which bucket' never becomes an argument — an id renamed"
                                + " later stops enforcing silently in every consumer that froze it.");
            }
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
            boolean metaCheck = segments.length == 2;

            assertTrue(marked || metaCheck,
                    () -> "id '" + id + "' has no polarity marker: a slug either starts with 'no-'"
                            + " (a prohibition) or contains '-must-' (an obligation, read as"
                            + " <subject>-must-<predicate>, e.g. fields-must-be-final)."
                            + " A depth-2 corral.<slug> meta-check like corral.exclusions-resolve is"
                            + " exempt — it asserts nothing about consumer code, so it carries none.");
        }
    }

    static void assertQualifierSegmentGrammar(Set<String> ids) {
        for (String id : ids) {
            String[] segments = id.split("\\.");

            if (segments.length == 4) {
                String qualifier = segments[2];

                assertTrue(QUALIFIERS.contains(qualifier),
                        () -> "id '" + id + "' qualifies with '" + qualifier + "'. A fourth segment"
                                + " may only be a library " + QUALIFIERS + " — a sub-topic belongs in"
                                + " a group, which can be reorganised, not in the id, which cannot.");
            }
        }
    }
}
