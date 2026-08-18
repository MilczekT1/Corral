package io.github.milczekt1.llamaguard.doc;

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
 * @param howNotToFix rule-specific anti-fix guidance, or {@code null} when the rule adds none;
 *                    blank text is normalised to {@code null}
 */
@Builder(builderClassName = "Builder")
public record RuleDoc(String id, String why, String howToFix, String howNotToFix) {

    /** Lower-case, dot-namespaced, kebab-cased segments — e.g. {@code db.no-spring-transactional}. */
    private static final Pattern ID_PATTERN = Pattern.compile("^[a-z0-9]+(\\.[a-z0-9-]+)+$");

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
    }

    private static String trimmedOrNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

}
