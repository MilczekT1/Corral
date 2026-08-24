package io.github.milczekt1.corral.exclude;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.Priority;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.doc.RuleRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

/**
 * Reads {@value #EXCLUSIONS_FILE} off the classpath: the rules this codebase permanently opts out
 * of, because they contradict how it is built.
 *
 * <p>Subtractive, not additive. A consumer keeps {@code ArchTests.in(AllCentralRules.class)} — and
 * so keeps receiving rules as the catalog grows — having removed one rule rather than opted out of
 * the mechanism that delivers them.
 *
 * <p>Nothing here throws. Loading happens in a static initialiser, and an exception there reaches
 * the consumer as {@code failed to discover tests} with the cause dropped, so every failure becomes
 * a {@link Loaded#problem()} that later fails every rule with a message instead.
 */
@UtilityClass
public class RuleExclusions {

    /** Public so a failure message can name the file it is telling the reader about. */
    public static final String EXCLUSIONS_FILE = "corral-exclusions.txt";

    /** Between the id and its reason. Two characters no rule id and no sentence starts with. */
    private static final String SEPARATOR = "::";

    private static final String COMMENT = "#";

    /** Read once per JVM, on the first {@code guard()} call — one classpath walk for any number of rules. */
    private static final Loaded STATE = load(currentClassLoader());

    /**
     * What the classpath amounts to: the exclusions in effect, or the reason there are none to be
     * trusted. Exactly one of the two is populated — a file that cannot be understood excludes
     * nothing, so a broken line can never silence a rule by accident.
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

    /** Whether the classpath carries an exclusions file that could not be understood. */
    static boolean isBroken() {
        return STATE.isBroken();
    }

    /**
     * {@code rule} unchanged when there is no file at all — which is every rule, for every consumer
     * who wrote none. Otherwise the rule is wrapped: excluded rules evaluate nothing and pass, and
     * every rule carries the unknown-id check, which cannot live anywhere else because an id naming
     * no rule is by definition attached to no rule.
     *
     * <p>Applied <em>outside</em> {@code FreezingArchRule}, so an excluded rule never reaches the
     * freeze store. A rule re-recorded as clean would have its debt entries deleted, and re-enabling
     * it would resurface every one of them as new.
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
        return state.excludes(ruleId) ? new Excluded(rule.getDescription(), state) : new Checked(rule, state);
    }

    /**
     * The id in the file must be one some rule registers. A typo would otherwise exclude nothing
     * while reading as though it did, and a renamed rule would silently come back on.
     *
     * @return the problem, or {@code null} when every excluded id is registered
     */
    static String unknownIdProblem(Loaded state, Collection<String> registeredIds) {
        List<String> unknown = state.entries().stream()
                .map(Exclusion::ruleId)
                .filter(id -> !registeredIds.contains(id))
                .toList();

        return unknown.isEmpty() ? null : """
                %s excludes %s, which no rule registers under that id. An exclusion that names \
                nothing removes nothing, and reads in the diff as though it did — so it fails rather \
                than passing quietly. Either the id is a typo, or the rule was renamed and its \
                exclusion needs renaming with it.
                Registered ids:
                %s\
                """.formatted(EXCLUSIONS_FILE, quoted(unknown), listedText(registeredIds.stream().sorted().toList()));
    }

    /**
     * Plural: {@code getResource} would resolve first-match-wins, and the copy doing the excluding
     * could be one a transitive test-scoped dependency shipped rather than one the consumer wrote.
     */
    static Loaded load(ClassLoader classLoader) {
        List<URL> locations;
        try {
            locations = Collections.list(classLoader.getResources(EXCLUSIONS_FILE));
        } catch (IOException e) {
            // A classpath that cannot be enumerated cannot be declared free of exclusions either.
            return Loaded.broken("Could not scan the classpath for " + EXCLUSIONS_FILE + ": " + e);
        }

        if (locations.isEmpty()) {
            return Loaded.none();
        }
        if (locations.size() > 1) {
            return Loaded.broken(ambiguousClassPathMessage(locations));
        }

        URL only = locations.get(0);
        try (InputStream contents = only.openStream()) {
            return parse(new String(contents.readAllBytes(), UTF_8), only.toString());
        } catch (IOException e) {
            return Loaded.broken("Could not read " + only + ": " + e);
        }
    }

    /**
     * Every broken line is reported, not only the first — fixing the file one round trip per line is
     * how a consumer ends up deleting it instead.
     */
    static Loaded parse(String contents, String source) {
        List<Exclusion> entries = new ArrayList<>();
        Set<String> problems = new LinkedHashSet<>();
        Set<String> idsSeen = new LinkedHashSet<>();
        int lineNumber = 0;

        for (String rawLine : contents.lines().toList()) {
            lineNumber++;
            String line = rawLine.strip();
            if (line.isEmpty() || line.startsWith(COMMENT)) {
                continue;
            }
            try {
                Exclusion exclusion = parseLine(line);
                if (!idsSeen.add(exclusion.ruleId())) {
                    throw new IllegalArgumentException("'" + exclusion.ruleId()
                            + "' is excluded more than once — which reason is in effect is unanswerable");
                }
                entries.add(exclusion);
            } catch (IllegalArgumentException e) {
                problems.add("  " + source + ":" + lineNumber + " — " + e.getMessage()
                        + System.lineSeparator() + "      " + line);
            }
        }

        return problems.isEmpty()
                ? Loaded.of(entries)
                : Loaded.broken(brokenFileMessage(source, problems));
    }

    private static Exclusion parseLine(String line) {
        int separator = line.indexOf(SEPARATOR);
        if (separator < 0) {
            throw new IllegalArgumentException(
                    "no '" + SEPARATOR + "' — every exclusion states a reason: <rule-id> "
                            + SEPARATOR + " <reason>");
        }
        return new Exclusion(
                line.substring(0, separator).strip(),
                line.substring(separator + SEPARATOR.length()).strip());
    }

    private static String brokenFileMessage(String source, Set<String> problems) {
        return """
                %s could not be read, so no rule is excluded and every Corral rule fails until it is \
                fixed. A file that is not understood must not be trusted to remove a rule.
                %s
                Each line is either blank, a '%s' comment, or '<rule-id> %s <reason>'.\
                """.formatted(source, String.join(System.lineSeparator(), problems), COMMENT, SEPARATOR);
    }

    private static String ambiguousClassPathMessage(List<URL> locations) {
        return """
                %s is on the classpath more than once, so which rules it removes is decided by \
                classpath order rather than by you. One of these may belong to a test-scoped \
                dependency rather than to this build.
                Found:
                %s
                Keep exactly one, in this module's own test resources.\
                """.formatted(EXCLUSIONS_FILE, listed(locations));
    }

    private static String listed(List<URL> locations) {
        return listedText(locations.stream().map(URL::toString).toList());
    }

    private static String listedText(Collection<String> lines) {
        return lines.isEmpty()
                ? "  (none — no rule has registered yet)"
                : lines.stream().map(line -> "  " + line).collect(joining(System.lineSeparator()));
    }

    private static String quoted(Collection<String> ids) {
        return ids.stream().map(id -> "'" + id + "'").collect(joining(", "));
    }

    /**
     * Renders every exclusion in effect, so a reader of a failing build sees what is not being
     * enforced. Empty when there are none — the common case, where the block is omitted entirely.
     */
    public static List<String> census() {
        return inEffect().stream()
                .map(exclusion -> exclusion.ruleId() + " :: " + exclusion.reason())
                .toList();
    }

    /**
     * Deferred to evaluation on purpose: at {@code guard()} time only the rules loaded so far have
     * registered, so no id can be judged unknown yet. By the time any rule evaluates, ArchUnit has
     * resolved every {@code @ArchTest} field in the tree and the registry is complete.
     *
     * <p>Run by excluded and unexcluded rules alike, because an id that names nothing is by
     * definition attached to no rule — there is no other place it could be noticed.
     */
    private static void failOnUnknownIds(Loaded state) {
        String problem = unknownIdProblem(state, RuleRegistry.all().stream().map(RuleDoc::id).toList());
        if (problem != null) {
            throw new AssertionError(problem);
        }
    }

    /**
     * A rule the file removes. Evaluates nothing: it does not merely find no violations, it never
     * looks, so no predicate runs and no violation line is produced for the store to record.
     */
    @RequiredArgsConstructor
    private static final class Excluded implements ArchRule {

        private final String description;
        private final Loaded state;

        @Override
        public void check(JavaClasses classes) {
            failOnUnknownIds(state);
        }

        @Override
        public EvaluationResult evaluate(JavaClasses classes) {
            failOnUnknownIds(state);
            return new EvaluationResult(this, Priority.MEDIUM);
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public ArchRule as(String newDescription) {
            return new Excluded(newDescription, state);
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

    /**
     * A rule no line names, carrying only the unknown-id check. Delegates everything else verbatim —
     * the violation lines it produces are the freeze store's matching keys and must not change.
     */
    @RequiredArgsConstructor
    private static final class Checked implements ArchRule {

        private final ArchRule delegate;
        private final Loaded state;

        @Override
        public void check(JavaClasses classes) {
            failOnUnknownIds(state);
            delegate.check(classes);
        }

        @Override
        public EvaluationResult evaluate(JavaClasses classes) {
            failOnUnknownIds(state);
            return delegate.evaluate(classes);
        }

        @Override
        public String getDescription() {
            return delegate.getDescription();
        }

        @Override
        public ArchRule as(String newDescription) {
            return new Checked(delegate.as(newDescription), state);
        }

        @Override
        public ArchRule because(String reason) {
            return new Checked(delegate.because(reason), state);
        }

        @Override
        public ArchRule allowEmptyShould(boolean allowEmptyShould) {
            return new Checked(delegate.allowEmptyShould(allowEmptyShould), state);
        }
    }

    /**
     * Every rule, when the file cannot be understood. Not a rule that fails — a rule that cannot be
     * evaluated, because a file that is not understood must not be trusted to remove anything.
     * Nothing a caller chains softens it.
     */
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

    /** Mirrors {@code ClassLoaders.getCurrentClassLoader}, which is how ArchUnit resolves its own files. */
    static ClassLoader currentClassLoader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        return contextClassLoader != null ? contextClassLoader : RuleExclusions.class.getClassLoader();
    }
}
