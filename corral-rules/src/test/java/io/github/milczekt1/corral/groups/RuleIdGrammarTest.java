package io.github.milczekt1.corral.groups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.milczekt1.corral.doc.DeprecatedRule;
import io.github.milczekt1.corral.reflect.PublishedRules;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Enforces the shape of every id Corral itself publishes: {@code corral} at segment 1, a closed
 * concern vocabulary at segment 2, a polarity marker on the slug. Checked against
 * {@link EveryPublishedGroup} only — {@code RuleDoc} applies the hygiene caps a consumer's own id
 * needs.
 *
 * <p>A retired id (see {@link DeprecatedRule#retiredIds()}) is exempt from the namespace and polarity
 * checks, but not from the qualifier-segment check.
 *
 * <p>Those two read {@link DeprecatedRule#retiredIds()} only after {@link #publishedIds()} has run,
 * which is what forces every retirement to register. Do not hoist the call.
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
        return PublishedRules.idsOf(EveryPublishedGroup.class);
    }

    /** Guards the three tests below, whose loops would pass vacuously over an empty catalog. */
    @Test
    void publishedIdsIsNotEmpty() {
        assertFalse(publishedIds().isEmpty(),
                "EveryPublishedGroup published no id at all — the other tests here iterate this same set"
                        + " and would pass vacuously if it were empty");
    }

    @Test
    void everyPublishedIdStartsWithTheVendorPrefixAndAClosedConcern() {
        assertNamespaceGrammar(publishedIds());
    }

    @Test
    void everyPublishedSlugCarriesAPolarityMarker() {
        assertPolarityGrammar(publishedIds());
    }

    @Test
    void aFourthSegmentIsOnlyALibraryQualifier() {
        assertQualifierSegmentGrammar(publishedIds());
    }

    /** Package-private so {@code DeprecatedRuleRetirementTest} runs this same check on a fixture. */
    static void assertNamespaceGrammar(Set<String> ids) {
        Set<String> retiredIds = DeprecatedRule.retiredIds();
        for (String id : ids) {
            if (retiredIds.contains(id)) {
                continue; // predates the grammar, including the vendor prefix; see DeprecatedRule.retiredIds().
            }
            String[] segments = id.split("\\.");
            String prefix = segments[0];

            assertEquals(VENDOR_PREFIX, prefix,
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

            assertTrue(marked,
                    () -> "id '" + id + "' has no polarity marker: a slug either starts with 'no-'"
                            + " (a prohibition) or contains '-must-' (an obligation, read as"
                            + " <subject>-must-<predicate>, e.g. fields-must-be-final). Every"
                            + " published id needs one, including a depth-2 corral.<slug> framework"
                            + " meta-check, were one ever published.");
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
