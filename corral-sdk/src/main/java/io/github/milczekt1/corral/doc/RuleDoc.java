package io.github.milczekt1.corral.doc;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Builder;

/**
 * Structured, agent-facing documentation for a single architecture rule.
 *
 * <p>{@link #id()} becomes the ArchUnit rule description, which is both the freeze-store key and how
 * {@code AgentFriendlyFailureDisplayFormat} finds this doc again. Prose is kept out of it so that
 * rewording documentation cannot re-seed every consumer's store.
 *
 * <p><strong>Changing an id is a breaking change.</strong>
 *
 * @param howNotToFix rule-specific anti-fix guidance, or {@code null} when the rule adds none;
 *                    blank text is normalised to {@code null}
 */
@Builder(builderClassName = "Builder")
public record RuleDoc(String id, String why, String howToFix, String howNotToFix) {

    /**
     * Lower-case, dot-namespaced, kebab-cased segments — e.g. {@code logging.no-system-out}.
     *
     * <p>Quantifiers are possessive throughout. Nothing here needs to backtrack — a segment stops at
     * the dot it cannot match — and Java implements greedy repetition of a group by recursing once
     * per iteration, so the greedy spelling would blow the stack on a long enough input.
     */
    private static final Pattern ID_PATTERN = Pattern.compile("^[a-z0-9]++(?:\\.[a-z0-9-]++)++$");

    /** Segment-1 values, closed on purpose — see CONTRIBUTING. {@code java<N>} is matched separately. */
    private static final Set<String> NAMESPACES = Set.of(
            "api", "concurrency", "corral", "exception", "jackson", "jakarta", "java",
            "layering", "logging", "lombok", "naming", "security", "spring", "test");

    /** A JDK-version namespace, for a rule about an API that exists only from that release. */
    private static final Pattern JAVA_VERSION = Pattern.compile("^java[0-9]++$");

    /** Second segments allowed to precede the slug: a library, or a JDK version. */
    private static final Set<String> QUALIFIERS = Set.of("mockito", "powermock", "junit");

    /** Corral's own meta-checks assert nothing about consumer code, so they carry no polarity marker. */
    private static final String RESERVED = "corral";

    private static final int MAX_ID_LENGTH = 60;

    public RuleDoc {
        requireText(id, "id");
        requireText(why, "why");
        requireText(howToFix, "howToFix");
        throwOnInvalidId(id);
        howNotToFix = trimmedOrNull(howNotToFix);
    }

    public boolean hasHowNotToFix() {
        return howNotToFix != null;
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be null or blank");
        }
    }

    /** Whether {@code text} has the shape of an id, where an id must be told apart from prose. */
    public static boolean isId(String text) {
        return text != null && ID_PATTERN.matcher(text).matches();
    }

    private static void throwOnInvalidId(String id) {
        if (!ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException(
                    "id '" + id + "' must match " + ID_PATTERN.pattern()
                            + " — it is the freeze-store key and must stay short and stable");
        }
        if (id.length() > MAX_ID_LENGTH) {
            throw new IllegalArgumentException("id '" + id + "' is " + id.length()
                    + " characters; the cap is " + MAX_ID_LENGTH + ". Shorten the slug rather than"
                    + " adding a segment — a segment is taxonomy in a key that can never change.");
        }

        String[] segments = id.split("\\.");
        String namespace = segments[0];
        if (!NAMESPACES.contains(namespace) && !JAVA_VERSION.matcher(namespace).matches()) {
            throw new IllegalArgumentException("id '" + id + "' starts with '" + namespace
                    + "', which is not a Corral namespace. Use one of " + sorted(NAMESPACES)
                    + ", or java<N> for a rule about an API introduced in that JDK. The list is closed"
                    + " so that 'which bucket' never becomes an argument — an id renamed later stops"
                    + " enforcing silently in every consumer that froze it.");
        }
        if (segments.length > 3) {
            throw new IllegalArgumentException("id '" + id + "' has " + segments.length
                    + " segments; the cap is 3.");
        }
        if (segments.length == 3 && !QUALIFIERS.contains(segments[1])) {
            throw new IllegalArgumentException("id '" + id + "' qualifies with '" + segments[1]
                    + "'. A third segment may only be a library " + sorted(QUALIFIERS) + " — a"
                    + " sub-topic belongs in a group, which can be reorganised, not in the id, which"
                    + " cannot.");
        }

        String slug = segments[segments.length - 1];
        boolean marked = slug.startsWith("no-") || slug.contains("-must-");
        if (!marked && !RESERVED.equals(namespace)) {
            throw new IllegalArgumentException("id '" + id + "' has no polarity marker: a slug either"
                    + " starts with 'no-' (a prohibition) or contains '-must-' (an obligation, read as"
                    + " <subject>-must-<predicate>, e.g. fields-must-be-final).");
        }
    }

    private static String sorted(Set<String> values) {
        return values.stream().sorted().collect(Collectors.joining(", ", "[", "]"));
    }

    private static String trimmedOrNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

}
