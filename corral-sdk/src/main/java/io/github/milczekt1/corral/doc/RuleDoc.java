package io.github.milczekt1.corral.doc;

import java.util.regex.Pattern;
import lombok.Builder;

/**
 * Structured, agent-facing documentation for a single architecture rule.
 *
 * <p>{@link #id()} becomes the ArchUnit rule description: the freeze-store key, and how
 * {@code AgentFriendlyFailureDisplayFormat} finds this doc again. <strong>Changing an id is a
 * breaking change.</strong>
 *
 * <p>Validated for shape, length and depth only — the id becomes a file name in every consumer's
 * freeze store. Namespace and slug taxonomy is Corral-catalog policy, enforced against this
 * library's own ids by {@code RuleIdGrammarTest}.
 *
 * @param howNotToFix rule-specific anti-fix guidance, or {@code null} when the rule adds none;
 *                    blank text is normalised to {@code null}
 */
@Builder(builderClassName = "Builder")
public record RuleDoc(String id, String why, String howToFix, String howNotToFix) {

    /**
     * Lower-case, dot-namespaced, kebab-cased segments — e.g. {@code acme.no-stdout-in-services}.
     * Possessive throughout: greedy group repetition recurses per iteration and overflows the stack.
     */
    private static final Pattern ID_PATTERN = Pattern.compile("^[a-z0-9]++(?:\\.[a-z0-9-]++)++$");

    /** 60 characters of taxonomy, plus room for a vendor prefix segment and its dot. */
    private static final int MAX_ID_LENGTH = 72;

    /** 3 taxonomy segments — namespace, concern/qualifier, slug — plus the vendor prefix. */
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
     * Whether {@code text} is a legal id and within the caps — the check to make before trusting it as
     * a file name. {@link #isId} applies shape only, so it admits an unbounded-length description.
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
