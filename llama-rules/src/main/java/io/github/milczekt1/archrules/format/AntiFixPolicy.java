package io.github.milczekt1.archrules.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The global "how NOT to fix this" policy rendered at the bottom of <em>every</em> framework
 * rule failure.
 *
 * <p>Rule authors and consumers may {@link #addClause append} project-specific clauses. There is
 * intentionally no API to remove or replace the baseline: the whole point is that the anti-cheat
 * guidance cannot be forgotten, weakened, or quietly dropped per rule.
 */
public final class AntiFixPolicy {

    private static final List<String> BASELINE = List.of(
            "Do NOT edit, hand-write, or delete files under archunit/frozen/ to make a NEW violation"
                    + " disappear. The store records pre-existing debt only; new violations must be fixed in code.",
            "Do NOT silence the rule with @SuppressWarnings, @ArchIgnore, comments, or by disabling the test.",
            "Do NOT rename a class, field, or package solely to dodge a name-based rule"
                    + " (e.g. renaming FooIT so the integration-test rule stops matching).",
            "Do NOT narrow @AnalyzeClasses(packages=...) or add ImportOptions to hide code from the scan.",
            "Do NOT downgrade, remove, reword, or otherwise weaken the rule.",
            "The ONLY acceptable resolution is changing the production/test code so the rule genuinely"
                    + " passes — then follow this rule's HOW TO FIX.");

    private static final List<String> ADDITIONAL = new CopyOnWriteArrayList<>();

    private AntiFixPolicy() {
    }

    /** Baseline clauses first, then any appended clauses, in insertion order. */
    public static List<String> clauses() {
        List<String> all = new ArrayList<>(BASELINE);
        all.addAll(ADDITIONAL);
        return Collections.unmodifiableList(all);
    }

    /** Appends a project-specific clause. The baseline is never affected. */
    public static void addClause(String clause) {
        if (clause == null || clause.isBlank()) {
            throw new IllegalArgumentException("clause must not be null or blank");
        }
        ADDITIONAL.add(clause.trim());
    }
}
