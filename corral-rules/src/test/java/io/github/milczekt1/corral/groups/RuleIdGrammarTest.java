package io.github.milczekt1.corral.groups;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.milczekt1.corral.reflect.PublishedRules;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Enforces the shape of every id Corral itself publishes.
 *
 * <p>{@code RuleDoc}'s own constructor checks only universal hygiene — shape, length, depth — because
 * it is public SDK surface: a consumer's own {@code DocumentedRule} builds a {@code RuleDoc} against
 * it too, with a namespace of their own choosing. What namespace a rule may claim, and what marks a
 * slug as a prohibition or an obligation, is catalog taxonomy for THIS project's rules, so it belongs
 * here — checked against {@link AllCentralRules}, the whole published catalog, and nowhere a
 * consumer's own id would ever trip it.
 *
 * <p>An id is the freeze-store key: it cannot change once a consumer has frozen it. The vocabulary
 * below is closed on purpose, so that "which bucket" never becomes an editorial argument —
 * relitigating it is exactly what produces a rename, and a rename silently stops enforcement in every
 * consumer that froze the old id.
 */
class RuleIdGrammarTest {

    /** Segment-1 values, closed on purpose — see CONTRIBUTING. {@code java<N>} is matched separately. */
    private static final Set<String> NAMESPACES = Set.of(
            "api", "concurrency", "corral", "exception", "jackson", "jakarta", "java",
            "layering", "logging", "lombok", "naming", "security", "spring", "test");

    /** A JDK-version namespace, for a rule about an API that exists only from that release. */
    private static final Pattern JAVA_VERSION = Pattern.compile("^java[0-9]++$");

    /** Second segments allowed to precede the slug: a library, or a JDK version. */
    private static final Set<String> QUALIFIERS = Set.of("mockito", "powermock", "junit");

    /**
     * Corral's own meta-checks — {@code corral.exclusions-resolve} is the sole one — assert nothing
     * about consumer code, so they carry no polarity marker.
     */
    private static final String RESERVED = "corral";

    private static Set<String> publishedIds() {
        return PublishedRules.idsOf(AllCentralRules.class);
    }

    @Test
    void everyPublishedIdStartsWithAClosedNamespace() {
        for (String id : publishedIds()) {
            String namespace = id.split("\\.")[0];

            assertTrue(NAMESPACES.contains(namespace) || JAVA_VERSION.matcher(namespace).matches(),
                    () -> "id '" + id + "' starts with '" + namespace + "', which is not a Corral"
                            + " namespace. Use one of " + NAMESPACES + ", or java<N> for a rule about"
                            + " an API introduced in that JDK. The list is closed so that 'which"
                            + " bucket' never becomes an argument — an id renamed later stops"
                            + " enforcing silently in every consumer that froze it.");
        }
    }

    @Test
    void everyPublishedSlugCarriesAPolarityMarkerExceptCorralsOwnMetaChecks() {
        for (String id : publishedIds()) {
            String[] segments = id.split("\\.");
            String namespace = segments[0];
            String slug = segments[segments.length - 1];
            boolean marked = slug.startsWith("no-") || slug.contains("-must-");

            assertTrue(marked || RESERVED.equals(namespace),
                    () -> "id '" + id + "' has no polarity marker: a slug either starts with 'no-'"
                            + " (a prohibition) or contains '-must-' (an obligation, read as"
                            + " <subject>-must-<predicate>, e.g. fields-must-be-final)."
                            + " corral.* is exempt — a framework meta-check like"
                            + " corral.exclusions-resolve asserts nothing about consumer code, so it"
                            + " carries none.");
        }
    }

    @Test
    void aThirdSegmentIsOnlyALibraryQualifier() {
        for (String id : publishedIds()) {
            String[] segments = id.split("\\.");

            if (segments.length == 3) {
                String qualifier = segments[1];

                assertTrue(QUALIFIERS.contains(qualifier),
                        () -> "id '" + id + "' qualifies with '" + qualifier + "'. A third segment"
                                + " may only be a library " + QUALIFIERS + " — a sub-topic belongs in"
                                + " a group, which can be reorganised, not in the id, which cannot.");
            }
        }
    }
}
