package io.github.milczekt1.corral.exclude;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.Priority;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.doc.RuleRegistry;
import io.github.milczekt1.corral.reflect.PublishedRules;
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
     * Documentation for {@link #resolvedAgainst}, which is published as an {@code @ArchTest} rule
     * and therefore owes consumers the same guidance every other rule gives.
     */
    private static final RuleDoc RESOLVE_DOC = RuleDoc.builder()
            .id("corral.exclusions-resolve")
            .why("""
                    An exclusion that names an id nothing publishes removes nothing, while reading in \
                    the diff exactly like one that does. That happens two ways: a typo, and a rule \
                    renamed upstream — the second is the dangerous one, because the rule you \
                    deliberately turned off comes back on at the next catalog upgrade and you find out \
                    from a failing build on code nobody touched.""")
            .howToFix("""
                    Correct the id to one this build actually wires — the failure lists them — or \
                    delete the line if the rule it named is gone. If it names a rule you wrote \
                    yourself, check that your own wiring evaluates it: a rule no @ArchTest field \
                    reaches is a rule nothing registers.""")
            .howNotToFix("""
                    Do NOT delete the line merely to make this check pass while still expecting the \
                    rule to be off. Deleting it turns the rule back ON, which is the opposite of what \
                    the line was for, and the rule's own violations are what you will see next.""")
            .build();

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
     * {@code rule} unchanged unless this file names it — so every rule a consumer does not exclude
     * is the same object, evaluated identically, whatever else the file says.
     *
     * <p>Deliberately does <em>not</em> validate that the ids resolve. At the moment one rule is
     * evaluated, {@link RuleRegistry} holds only the rules loaded so far, so a run wiring one group
     * would call every other group's exclusions typos and fail rules that are perfectly fine. That
     * check needs a complete set and lives in {@link #resolvedAgainst}.
     *
     * <p>Applied <em>outside</em> {@code FreezingArchRule}, so an excluded rule never reaches the
     * freeze store. A rule re-recorded as clean would have its debt entries deleted, and re-enabling
     * it would resurface every one of them as new.
     */
    public static ArchRule applyTo(ArchRule rule, String ruleId) {
        return applyTo(rule, ruleId, STATE);
    }

    /**
     * The unknown-id guardrail: a rule that fails when a line in the file names an id nothing
     * publishes or registers. Wire it beside the catalog root a consumer already evaluates.
     *
     * <p>Walking {@code wiredRoot} is what makes this sound. {@code PublishedRules} reads every
     * reachable {@code @ArchTest} field, which forces each rule class to initialise, so the ids it
     * yields are complete for that root by construction — unlike the registry mid-run.
     *
     * <p>The registry is unioned in anyway, for a consumer excluding a rule they wrote themselves:
     * no catalog root publishes it, but their own wiring has registered it by the time this runs.
     * That direction is best-effort by nature, and it can only ever let an id through, never
     * manufacture a failure.
     */
    public static ArchRule resolvedAgainst(Class<?> wiredRoot) {
        return resolvedAgainst(wiredRoot, STATE);
    }

    static ArchRule resolvedAgainst(Class<?> wiredRoot, Loaded state) {
        RuleRegistry.register(RESOLVE_DOC);
        return new Resolves(wiredRoot, state);
    }

    static ArchRule applyTo(ArchRule rule, String ruleId, Loaded state) {
        if (state.isBroken()) {
            return new Failing(rule.getDescription(), state.problem());
        }
        if (state.entries().isEmpty()) {
            return rule;
        }
        return state.excludes(ruleId) ? new Excluded(rule.getDescription()) : rule;
    }

    /**
     * Every excluded id that names nothing in {@code knownIds}. A typo would otherwise exclude
     * nothing while reading in the diff as though it did, and a rule renamed upstream would come
     * back on silently.
     *
     * @return the problem, or {@code null} when every excluded id resolves
     */
    static String unresolvedIdProblem(Loaded state, Collection<String> knownIds) {
        List<String> unresolved = state.entries().stream()
                .map(Exclusion::ruleId)
                .filter(id -> !knownIds.contains(id))
                .toList();

        return unresolved.isEmpty() ? null : """
                %s excludes %s, which nothing this build wires publishes or registers under that id. \
                An exclusion that names nothing removes nothing, and reads in the diff as though it \
                did — so it fails rather than passing quietly. Either the id is a typo, or the rule \
                was renamed and its exclusion needs renaming with it.
                Ids this build knows:
                %s\
                """.formatted(EXCLUSIONS_FILE, quoted(unresolved), listedText(knownIds.stream().sorted().toList()));
    }

    /**
     * Plural: {@code getResource} would resolve first-match-wins, and the copy doing the excluding
     * could be one a transitive test-scoped dependency shipped rather than one the consumer wrote.
     */
    static Loaded load(ClassLoader classLoader) {
        // Unchecked failures too, not only IOException: this runs in a static initialiser, so
        // anything escaping it becomes an ExceptionInInitializerError. That is an Error, so every
        // guard written against RuntimeException lets it straight through — including the ones
        // holding up the formatter's never-throw contract — and every later call gets
        // NoClassDefFoundError instead. A misbehaving URL handler is the realistic way in.
        try {
            return read(classLoader);
        } catch (IOException | RuntimeException e) {
            // A classpath that cannot be read cannot be declared free of exclusions either.
            return Loaded.broken("Could not read " + EXCLUSIONS_FILE + " from the classpath: " + e);
        }
    }

    private static Loaded read(ClassLoader classLoader) throws IOException {
        List<URL> locations = Collections.list(classLoader.getResources(EXCLUSIONS_FILE));

        if (locations.isEmpty()) {
            return Loaded.none();
        }
        if (locations.size() > 1) {
            return Loaded.broken(ambiguousClassPathMessage(locations));
        }

        URL only = locations.get(0);
        try (InputStream contents = only.openStream()) {
            return parse(new String(contents.readAllBytes(), UTF_8), only.toString());
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
     * A rule the file removes. Evaluates nothing: it does not merely find no violations, it never
     * looks, so no predicate runs and no violation line is produced for the store to record.
     */
    @RequiredArgsConstructor
    private static final class Excluded implements ArchRule {

        private final String description;

        @Override
        public void check(JavaClasses classes) {
            // Nothing to check: the rule is removed from this build, so no predicate runs.
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

    /**
     * The unknown-id guardrail as a rule, so it runs where the consumer already wires the catalog
     * and fails the build the way any other rule does.
     *
     * <p>Silent when the file could not be parsed: every rule is already failing with that problem,
     * and ids from a file nobody could read are not evidence of anything.
     */
    @RequiredArgsConstructor
    private static final class Resolves implements ArchRule {

        private final Class<?> wiredRoot;
        private final Loaded state;

        @Override
        public void check(JavaClasses classes) {
            if (state.isBroken() || state.entries().isEmpty()) {
                return;
            }
            String problem = unresolvedIdProblem(state, knownIds());
            if (problem != null) {
                throw new AssertionError(problem);
            }
        }

        /** The walk first: it initialises the rule classes whose registration the union relies on. */
        private Set<String> knownIds() {
            Set<String> known = new LinkedHashSet<>(PublishedRules.idsOf(wiredRoot));
            RuleRegistry.all().forEach(doc -> known.add(doc.id()));
            return known;
        }

        @Override
        public EvaluationResult evaluate(JavaClasses classes) {
            check(classes);
            return new EvaluationResult(this, Priority.MEDIUM);
        }

        @Override
        public String getDescription() {
            return RESOLVE_DOC.id();
        }

        @Override
        public ArchRule as(String newDescription) {
            return this;
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
