package io.github.milczekt1.llamarules;

import java.util.Optional;
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
 */
@Builder(builderClassName = "Builder")
public record RuleDoc(String id, String why, String howToFix, Optional<String> howNotToFix) {

    /** Lower-case, dot-namespaced, kebab-cased segments — e.g. {@code db.no-spring-transactional}. */
    private static final Pattern ID_PATTERN = Pattern.compile("^[a-z0-9]+(\\.[a-z0-9-]+)+$");

    public RuleDoc {
        requireText(id, "id");
        requireText(why, "why");
        requireText(howToFix, "howToFix");
        if (!ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException(
                    "id '" + id + "' must match " + ID_PATTERN.pattern()
                            + " — it is the freeze-store key and must stay short and stable");
        }
        if (howNotToFix == null) {
            throw new IllegalArgumentException("howNotToFix must not be null (use Optional.empty())");
        }
        howNotToFix = howNotToFix.map(String::trim).filter(s -> !s.isEmpty());
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be null or blank");
        }
    }

    /**
     * Partly hand-written; Lombok generates the rest. Two members are carried by hand because
     * Lombok's defaults are wrong here: the initialiser, because an omitted {@code Optional} would
     * arrive as {@code null} and trip the constructor's null check; and the {@code String} setter,
     * because the generated one takes {@code Optional<String>}. Lombok suppresses by method name, so
     * declaring the {@code String} form means no {@code Optional} overload is generated.
     */
    public static class Builder {

        private Optional<String> howNotToFix = Optional.empty();

        public Builder howNotToFix(String howNotToFix) {
            this.howNotToFix = Optional.ofNullable(howNotToFix);
            return this;
        }
    }
}
