package io.github.milczekt1.corral.format;

import java.util.List;
import lombok.experimental.UtilityClass;

/**
 * The global "how NOT to fix this" policy rendered at the bottom of <em>every</em> framework
 * rule failure.
 *
 * <p>Identical for every rule, and immutable: the anti-cheat guidance cannot be forgotten, weakened,
 * or quietly dropped. Clauses specific to one rule belong in that rule's
 * {@code RuleDoc.howNotToFix}, which renders just above this block.
 */
@UtilityClass
public class AntiFixPolicy {

    private static final List<String> BASELINE = List.of(
            """
                    Do NOT edit, hand-write, or delete files under archunit/frozen/ to make a NEW violation\
                     disappear. The store records pre-existing debt only; new violations must be fixed in code.""",
            """
                    Do NOT re-run with archunit.freeze.refreeze=true, and do NOT commit\
                     freeze.store.default.allowStoreCreation=true. Either one converts every current violation in\
                     every rule into accepted debt at once.""",
            """
                    Do NOT add to or create archunit_ignore_patterns.txt. ArchUnit discards anything matching that\
                     file before this rule, the freeze store, or this message ever sees it, leaving no record\
                     anywhere. Nothing in this catalog is exempted that way.""",
            "Do NOT silence the rule with @SuppressWarnings, @ArchIgnore, comments, or by disabling the test.",
            """
                    Do NOT rename a class, field, or package solely to dodge a name-based rule\
                     (e.g. renaming FooIT so the integration-test rule stops matching).""",
            "Do NOT narrow @AnalyzeClasses(packages=...) or add ImportOptions to hide code from the scan.",
            "Do NOT downgrade, remove, reword, or otherwise weaken the rule.",
            """
                    The ONLY acceptable resolution is changing the production/test code so the rule genuinely\
                     passes — then follow this rule's HOW TO FIX.""");

    public static List<String> clauses() {
        return BASELINE;
    }
}
