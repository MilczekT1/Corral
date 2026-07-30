package io.github.milczekt1.archrules;

import java.util.Optional;
import java.util.regex.Pattern;
import lombok.Builder;

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
     * Partially hand-written on purpose; Lombok generates the rest.
     *
     * <p>Two members are carried by hand because Lombok's defaults are wrong for this type: the
     * field initializer, because an omitted {@code Optional} component would otherwise arrive as
     * {@code null} and trip the canonical constructor's null check; and the {@code String} setter,
     * because the generated one would take {@code Optional<String>} and force every rule author to
     * write {@code .howNotToFix(Optional.of("..."))}. Lombok suppresses generation by method
     * <em>name</em>, so declaring the {@code String} form means no {@code Optional} overload is
     * generated at all.
     */
    public static class Builder {

        private Optional<String> howNotToFix = Optional.empty();

        public Builder howNotToFix(String howNotToFix) {
            this.howNotToFix = Optional.ofNullable(howNotToFix);
            return this;
        }
    }
}
