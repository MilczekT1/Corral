package io.github.milczekt1.archrules;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Structured, agent-facing documentation for a single architecture rule.
 *
 * <p>The {@link #id()} is deliberately short and stable: it becomes the ArchUnit rule
 * description, which in turn is the key ArchUnit uses for the freeze store <em>and</em> the key
 * {@code AgentFriendlyFailureDisplayFormat} uses to look this doc back up. Rich prose is kept
 * out of the description on purpose — rewording it would otherwise silently re-seed every
 * consumer's freeze store.
 *
 * <p><strong>Changing an id is a breaking change.</strong>
 */
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

    public static Builder builder() {
        return new Builder();
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be null or blank");
        }
    }

    public static final class Builder {
        private String id;
        private String why;
        private String howToFix;
        private String howNotToFix;

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder why(String why) {
            this.why = why;
            return this;
        }

        public Builder howToFix(String howToFix) {
            this.howToFix = howToFix;
            return this;
        }

        public Builder howNotToFix(String howNotToFix) {
            this.howNotToFix = howNotToFix;
            return this;
        }

        public RuleDoc build() {
            return new RuleDoc(id, why, howToFix, Optional.ofNullable(howNotToFix));
        }
    }
}
