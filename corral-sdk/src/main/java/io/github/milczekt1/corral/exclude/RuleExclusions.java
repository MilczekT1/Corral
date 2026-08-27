package io.github.milczekt1.corral.exclude;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.Priority;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
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
     * Every excluded id that has actually matched a rule this run evaluated, filled in by
     * {@link #applyTo} as rules pass through it. A {@link ConcurrentHashMap}-backed set, in the
     * style of {@code RuleRegistry}: rule classes initialise concurrently under a parallel Surefire
     * or Gradle test run.
     */
    private static final Set<String> MATCHED = ConcurrentHashMap.newKeySet();

    /** Flips once, at the first rule any consumer evaluates — shared by every {@link UnmatchedExclusionsWarning}. */
    private static final AtomicBoolean WARNED = new AtomicBoolean(false);

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

    /** For tests: whether {@code ruleId} has been recorded as having matched a rule this run. */
    static boolean hasMatched(String ruleId) {
        return MATCHED.contains(ruleId);
    }

    /**
     * {@code rule} unchanged unless this file names it — so every rule a consumer does not exclude
     * is the same object, evaluated identically, whatever else the file says.
     *
     * <p>Records a match in {@link #MATCHED} first, so {@link #warnUnmatchedExclusionsOnFirstEvaluation}
     * — evaluated later, once every rule class in the run has initialised and called this method — can
     * tell an id that matched nothing from one that did.
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
        if (!state.excludes(ruleId)) {
            return rule;
        }
        MATCHED.add(ruleId);
        return new Excluded(rule.getDescription());
    }

    /**
     * Wraps {@code rule} so that, on the first rule any consumer evaluates in this run, a warning is
     * printed for every excluded id that matched no rule — a typo, or a rule renamed or retired
     * upstream. Unlike a build failure, a warning can be honest about a partial run: "matched no rule
     * <em>in this run</em>" is true whether the run wires the whole catalog or a single leaf test, so
     * this needs no wired root and runs unconditionally from {@link io.github.milczekt1.corral.DocumentedRule#guard()}.
     *
     * <p>Fires once per run, not once per rule: {@link #WARNED} is a single static flag shared by
     * every wrapped rule. By the time any rule's {@code check}/{@code evaluate} actually runs, ArchUnit
     * has already finished walking every {@code @ArchTest} field it discovered, which forces every
     * wired rule class to initialise and call {@link #applyTo} — so {@link #MATCHED} is complete by
     * then, however early or late this particular rule happens to fire relative to its siblings.
     *
     * <p>{@code rule} unchanged when the file names no exclusion at all — the same pass-through
     * {@link #applyTo} uses for an empty file. There is nothing an id could fail to match, so
     * wrapping would only ever produce a silent no-op.
     */
    public static ArchRule warnUnmatchedExclusionsOnFirstEvaluation(ArchRule rule) {
        if (STATE.entries().isEmpty()) {
            return rule;
        }
        return warnUnmatchedExclusionsOnFirstEvaluation(
                rule, WARNED, RuleExclusions::unmatchedWarning, RuleExclusions::printWarning);
    }

    static ArchRule warnUnmatchedExclusionsOnFirstEvaluation(
            ArchRule rule, AtomicBoolean warned, Supplier<String> messageSupplier, Consumer<String> sink) {
        return new UnmatchedExclusionsWarning(rule, warned, messageSupplier, sink);
    }

    /**
     * Deliberately {@link System#err}, not SLF4J, even though {@code slf4j-api} is on this module's
     * classpath: this is a build-time diagnostic from a test-scoped library, and it is the entire
     * safety net for a silent misconfiguration, so it must not depend on the consumer having wired a
     * logging backend. Most consumers have not — a bare {@code slf4j-api} with no provider falls back
     * to a no-op logger, and {@code log.warn} here would vanish exactly as quietly as the typo it is
     * meant to surface. Maven and Gradle both capture and print stderr with nothing to configure.
     *
     * <p>Corral itself publishes {@code corral.logging.no-system-err}, which reads as an irony until
     * the target is named: that rule governs a <em>consumer's application code</em>, where stderr
     * bypasses whatever log pipeline the application runs in production. This is not application
     * code and there is no pipeline to bypass — it is the build tool telling the person running the
     * build something about the build, on the stream every build tool already surfaces.
     */
    static void printWarning(String message) {
        System.err.println(message);
    }

    /** Live at call time, deliberately: {@link #MATCHED} is still filling in while rules initialise. */
    private static String unmatchedWarning() {
        return unmatchedWarning(STATE, MATCHED);
    }

    /**
     * Every excluded id that has matched no rule seen so far. Honest about partial runs by
     * construction: it says nothing about ids no rule in {@code matched} names, only that none of the
     * rules this run evaluated up to now needed them — true on a full run and harmless on a single
     * leaf.
     *
     * @return the warning message, or {@code null} when every excluded id has matched something
     */
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
                """.formatted(EXCLUSIONS_FILE, unmatched.size(), unmatched.size() == 1 ? "" : "s",
                        unmatched.stream().map(id -> "  " + id).collect(joining(System.lineSeparator())));
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

        for (String rawLine : withoutByteOrderMark(contents).lines().toList()) {
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

    /**
     * {@code String.strip()} leaves it: {@code Character.isWhitespace('\uFEFF')} is false. Left in
     * place, a first line written by an editor that emits a BOM matches neither a comment nor an id,
     * and the whole file is rejected over a character invisible in the message quoting it.
     */
    private static String withoutByteOrderMark(String contents) {
        return contents.startsWith("\uFEFF") ? contents.substring(1) : contents;
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
     * A pass-through that, on the first {@code check}/{@code evaluate} across every rule sharing its
     * {@link #warned} flag, logs whatever {@link #messageSupplier} returns and then always delegates —
     * this never changes the outcome of the rule it wraps, only what reaches the log once.
     *
     * <p>Unlike {@link Excluded} and {@link Failing}, every method genuinely delegates: this rule is
     * not standing in for the one it wraps, it is riding along with it, so {@code because} and
     * {@code allowEmptyShould} must still reach the real rule, and {@code as} must keep wrapping
     * whatever {@code as} on the delegate returns.
     */
    @RequiredArgsConstructor
    private static final class UnmatchedExclusionsWarning implements ArchRule {

        private final ArchRule delegate;
        private final AtomicBoolean warned;
        private final Supplier<String> messageSupplier;
        private final Consumer<String> sink;

        @Override
        public void check(JavaClasses classes) {
            warnOnce();
            delegate.check(classes);
        }

        @Override
        public EvaluationResult evaluate(JavaClasses classes) {
            warnOnce();
            return delegate.evaluate(classes);
        }

        private void warnOnce() {
            if (!warned.compareAndSet(false, true)) {
                return;
            }
            String message = messageSupplier.get();
            if (message != null) {
                sink.accept(message);
            }
        }

        @Override
        public String getDescription() {
            return delegate.getDescription();
        }

        @Override
        public ArchRule as(String newDescription) {
            return new UnmatchedExclusionsWarning(delegate.as(newDescription), warned, messageSupplier, sink);
        }

        @Override
        public ArchRule because(String reason) {
            return new UnmatchedExclusionsWarning(delegate.because(reason), warned, messageSupplier, sink);
        }

        @Override
        public ArchRule allowEmptyShould(boolean allowEmptyShould) {
            return new UnmatchedExclusionsWarning(
                    delegate.allowEmptyShould(allowEmptyShould), warned, messageSupplier, sink);
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
