package io.github.milczekt1.corral.exclude;

import static java.util.stream.Collectors.joining;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.Priority;
import io.github.milczekt1.corral.settings.CorralSettings;
import io.github.milczekt1.corral.settings.SettingsFile;
import io.github.milczekt1.corral.warn.FirstEvaluationWarning;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

/**
 * The {@code corral.exclude.<rule-id>} entries of {@code corral.properties}: the rules this codebase
 * permanently opts out of. A broken settings file becomes a {@link Loaded#problem()} that fails every
 * rule with a message.
 */
@UtilityClass
public class RuleExclusions {

    /** Read once per JVM, on the first {@code guard()} call. */
    private static final Loaded STATE = from(CorralSettings.current());

    /** Excluded ids that matched a rule this run, filled in by {@link #applyTo}. */
    private static final Set<String> MATCHED = ConcurrentHashMap.newKeySet();

    /**
     * The exclusions in effect, or the reason there are none to be trusted. Exactly one is populated.
     *
     * @param problem {@code null} when the file was absent or read cleanly
     */
    record Loaded(List<Exclusion> entries, String problem) {

        Loaded {
            entries = List.copyOf(entries);
        }

        static Loaded none() {
            return new Loaded(List.of(), null);
        }

        static Loaded of(List<Exclusion> entries) {
            return new Loaded(entries, null);
        }

        static Loaded broken(String problem) {
            return new Loaded(List.of(), problem);
        }

        boolean isBroken() {
            return problem != null;
        }

        boolean excludes(String ruleId) {
            return entries.stream().anyMatch(exclusion -> exclusion.ruleId().equals(ruleId));
        }
    }

    /** The exclusions in effect; empty when there is no file, or when the file could not be read. */
    static List<Exclusion> inEffect() {
        return STATE.entries();
    }

    static boolean isBroken() {
        return STATE.isBroken();
    }

    static boolean hasMatched(String ruleId) {
        return MATCHED.contains(ruleId);
    }

    /**
     * {@code rule} unchanged unless the file names it; a match is recorded in {@link #MATCHED}.
     *
     * <p>Apply outside {@code FreezingArchRule} — an excluded rule must never reach the freeze store.
     */
    public static ArchRule applyTo(ArchRule rule, String ruleId) {
        return applyTo(rule, ruleId, STATE);
    }

    static ArchRule applyTo(ArchRule rule, String ruleId, Loaded state) {
        if (state.isBroken()) {
            return new Failing(rule.getDescription(), state.problem());
        }
        if (state.entries().isEmpty()) {
            return rule;
        }
        if (!state.excludes(ruleId)) {
            return rule;
        }
        MATCHED.add(ruleId);
        return new Excluded(rule.getDescription());
    }

    /**
     * Wraps {@code rule} so the first rule evaluated this run warns about every excluded id that
     * matched no rule. {@code rule} unchanged when the file names no exclusion at all.
     *
     * <p>{@link io.github.milczekt1.corral.DocumentedRule#guard()} already warns; do not wrap a
     * guarded rule again.
     */
    public static ArchRule warnUnmatchedExclusionsOnFirstEvaluation(ArchRule rule) {
        if (STATE.entries().isEmpty()) {
            return rule;
        }
        return FirstEvaluationWarning.wrap(rule, List.of(RuleExclusions::unmatchedWarning));
    }

    /**
     * The excluded ids that matched no rule this run, or {@code null} when there are none. Live at
     * call time: {@link #MATCHED} is still filling in while rules initialise.
     */
    public static String unmatchedWarning() {
        return unmatchedWarning(STATE, MATCHED);
    }

    /** @return the warning message, or {@code null} when every excluded id has matched something */
    static String unmatchedWarning(Loaded state, Collection<String> matched) {
        List<String> unmatched = state.entries().stream()
                .map(Exclusion::ruleId)
                .filter(id -> !matched.contains(id))
                .toList();

        if (unmatched.isEmpty()) {
            return null;
        }
        return """
                %s: %d exclusion%s matched no rule in this run:
                %s
                If you ran a single rule this is expected. In a full run it means the id names \
                nothing — a typo, or a rule renamed or retired upstream — so it removes nothing \
                while reading in the diff as though it did.\
                """.formatted(CorralSettings.FILE, unmatched.size(), unmatched.size() == 1 ? "" : "s",
                        unmatched.stream().map(id -> "  " + id).collect(joining(System.lineSeparator())));
    }

    static Loaded from(SettingsFile settings) {
        if (settings.isBroken()) {
            return Loaded.broken(settings.problem());
        }
        return Loaded.of(settings.exclusionReasons().entrySet().stream()
                .map(entry -> new Exclusion(entry.getKey(), entry.getValue()))
                .toList());
    }

    /** Every exclusion in effect, as its line in the settings file; empty when there are none. */
    public static List<String> census() {
        return census(STATE);
    }

    static List<String> census(Loaded state) {
        return state.entries().stream()
                .map(exclusion -> CorralSettings.EXCLUDE_PREFIX + exclusion.ruleId() + "=" + exclusion.reason())
                .toList();
    }

    /** A rule the file removes. Never looks, so no violation line reaches the store. */
    @RequiredArgsConstructor
    private static final class Excluded implements ArchRule {

        private final String description;

        @Override
        public void check(JavaClasses classes) {
            // Removed from this build.
        }

        @Override
        public EvaluationResult evaluate(JavaClasses classes) {
            return new EvaluationResult(this, Priority.MEDIUM);
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public ArchRule as(String newDescription) {
            return new Excluded(newDescription);
        }

        @Override
        public ArchRule because(String reason) {
            return this;
        }

        @Override
        public ArchRule allowEmptyShould(boolean allowEmptyShould) {
            return this;
        }
    }

    /** Every rule, when the file cannot be understood. Nothing a caller chains softens it. */
    @RequiredArgsConstructor
    private static final class Failing implements ArchRule {

        private final String description;
        private final String message;

        @Override
        public void check(JavaClasses classes) {
            throw new AssertionError(message);
        }

        @Override
        public EvaluationResult evaluate(JavaClasses classes) {
            throw new AssertionError(message);
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public ArchRule as(String newDescription) {
            return new Failing(newDescription, message);
        }

        @Override
        public ArchRule because(String reason) {
            return this;
        }

        @Override
        public ArchRule allowEmptyShould(boolean allowEmptyShould) {
            return this;
        }
    }
}
