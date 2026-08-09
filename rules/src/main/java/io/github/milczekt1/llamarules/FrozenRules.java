package io.github.milczekt1.llamarules;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import lombok.experimental.UtilityClass;

/**
 * Turns a raw {@link ArchRule} into the shape every central rule must have.
 *
 * <p>{@link #freeze} does three things, and the order matters:
 * <ol>
 *   <li>registers the {@link RuleDoc} so the failure formatter can look the prose back up;</li>
 *   <li>pins the rule description to the doc's short, stable {@code id} — ArchUnit derives the
 *       freeze-store key from the description, so this is what stops a reworded sentence from
 *       silently re-seeding every consumer's store;</li>
 *   <li>allows an empty {@code should}, so a module containing no matching classes stays green
 *       instead of failing vacuously.</li>
 * </ol>
 * Only then is the rule wrapped in a {@link FreezingArchRule}, so that adopting a new rule records
 * existing debt rather than blocking in-flight work.
 */
@UtilityClass
public class FrozenRules {

    public static ArchRule freeze(ArchRule rule, RuleDoc doc) {
        RuleRegistry.register(doc);
        return FreezingArchRule.freeze(rule.as(doc.id()).allowEmptyShould(true));
    }
}
