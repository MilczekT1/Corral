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

    /**
     * Segment 1 of an id is a vendor prefix (e.g. {@code corral}), not taxonomy, so it is not part of
     * the budget a rule author spends — it is simply added on top. 60 was the old cap with no prefix;
     * this is that budget plus room for a short, stable vendor segment and its dot, with headroom to
     * spare rather than none.
     */
    private static final int MAX_ID_LENGTH = 72;

    /**
     * Segment 1 of an id is a vendor prefix (e.g. {@code corral}), not taxonomy. The taxonomy budget
     * a rule author spends is unchanged at 3 segments — namespace, and up to two more for
     * concern/qualifier/slug shape — so this cap is 3 plus the one prefix segment.
     */
    private static final int MAX_ID_SEGMENTS = 4;

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

    /**
     * Whether {@code text} is a legal id AND obeys the length and segment caps — the stronger check a
     * caller needs before trusting {@code text} as something bounded, such as a file name.
     *
     * <p>{@link #isId} alone is not that check: it applies only {@code ID_PATTERN}, not
     * {@code MAX_ID_LENGTH} or {@code MAX_ID_SEGMENTS}, because it also backs the constructor's shape
     * error, which must fire on shape alone so its message stays specific. A rule frozen without a
     * {@code RuleDoc} — see {@code EmptyOmittingViolationStore} — can carry a description that merely
     * looks dot/kebab-shaped; gated on {@link #isId} alone, that description becomes an unbounded-length
     * file name.
     */
    public static boolean isIdWithinCaps(String text) {
        return isId(text) && text.length() <= MAX_ID_LENGTH && text.split("\\.").length <= MAX_ID_SEGMENTS;
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
