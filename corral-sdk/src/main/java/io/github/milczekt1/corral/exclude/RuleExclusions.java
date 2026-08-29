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
 * Reads {@value #EXCLUSIONS_FILE} off the classpath: the rules this codebase permanently opts out of.
 *
 * <p>Never throws — loading runs in a static initialiser, so a failure becomes a
 * {@link Loaded#problem()} that fails every rule with a message.
 */
@UtilityClass
public class RuleExclusions {

    public static final String EXCLUSIONS_FILE = "corral-exclusions.txt";

    private static final String SEPARATOR = "::";

    private static final String COMMENT = "#";

    /** Read once per JVM, on the first {@code guard()} call. */
    private static final Loaded STATE = load(currentClassLoader());

    /** Excluded ids that matched a rule this run, filled in by {@link #applyTo}. */
    private static final Set<String> MATCHED = ConcurrentHashMap.newKeySet();

    /** Flips at the first rule evaluated this run, shared by every {@link UnmatchedExclusionsWarning}. */
    private static final AtomicBoolean WARNED = new AtomicBoolean(false);

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
     * matched no rule. Fires once per run via {@link #WARNED}.
     *
     * <p>Assumes one JVM and one discovery pass — under {@code forkCount > 1} or a sharded CI a
     * correct exclusion can be reported unmatched.
     *
     * <p>{@code rule} unchanged when the file names no exclusion at all.
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

    /** {@link System#err}, not SLF4J: a consumer with no logging provider would never see this. */
    static void printWarning(String message) {
        System.err.println(message);
    }

    /** Live at call time: {@link #MATCHED} is still filling in while rules initialise. */
    private static String unmatchedWarning() {
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
                """.formatted(EXCLUSIONS_FILE, unmatched.size(), unmatched.size() == 1 ? "" : "s",
                        unmatched.stream().map(id -> "  " + id).collect(joining(System.lineSeparator())));
    }

    static Loaded load(ClassLoader classLoader) {
        // RuntimeException too: this runs in a static initialiser, where anything escaping becomes an Error.
        try {
            return read(classLoader);
        } catch (IOException | RuntimeException e) {
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

    /** Reports every broken line, not only the first. */
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

    /** {@code String.strip()} leaves it: {@code Character.isWhitespace('\uFEFF')} is false. */
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

    /** Every exclusion in effect as {@code <id> :: <reason>}; empty when there are none. */
    public static List<String> census() {
        return inEffect().stream()
                .map(exclusion -> exclusion.ruleId() + " :: " + exclusion.reason())
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

    /**
     * Logs {@link #messageSupplier} once across every rule sharing {@link #warned}, then delegates.
     *
     * <p>Every method delegates — unlike {@link Excluded} and {@link Failing}, this rides along with
     * the rule it wraps rather than standing in for it.
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

    /** Mirrors {@code ClassLoaders.getCurrentClassLoader}, how ArchUnit resolves its own files. */
    static ClassLoader currentClassLoader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        return contextClassLoader != null ? contextClassLoader : RuleExclusions.class.getClassLoader();
    }
}
