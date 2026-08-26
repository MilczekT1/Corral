package io.github.milczekt1.corral.doc;

import java.util.regex.Pattern;
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
 * <p>The checks here are deliberately limited to universal hygiene: shape, length, and depth. The id
 * becomes a file name in every consumer's freeze store — see {@code EmptyOmittingViolationStore} — so
 * those are filesystem and readability constraints that bind any rule author, this library's own
 * catalog and a consumer's own {@code DocumentedRule} alike. What a namespace may be, and what marks a
 * slug as a prohibition or an obligation, is Corral-catalog taxonomy, not a hygiene rule every consumer
 * must submit to — see {@code corral-rules}' own {@code RuleIdGrammarTest}, which enforces that only
 * against the ids this library itself publishes.
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

    private static final int MAX_ID_LENGTH = 60;

    private static final int MAX_ID_SEGMENTS = 3;

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

        int segmentCount = id.split("\\.").length;
        if (segmentCount > MAX_ID_SEGMENTS) {
            throw new IllegalArgumentException("id '" + id + "' has " + segmentCount
                    + " segments; the cap is " + MAX_ID_SEGMENTS + ". The id becomes a file name in"
                    + " every consumer's freeze store — a deep hierarchy belongs in a group, which can"
                    + " be reorganised, not in the id, which cannot.");
        }
    }

    private static String trimmedOrNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

}
