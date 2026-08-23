package io.github.milczekt1.corral.guard;

import static java.util.stream.Collectors.joining;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

/**
 * Fails the build when {@value #IGNORE_PATTERNS_FILE} is on the classpath.
 *
 * <p>ArchUnit reads that file inside {@code EvaluationResult}'s constructor and discards every
 * violation matching a regex in it — before {@code FreezingArchRule}, before the freeze store,
 * before any failure message. It is global and it leaves no evidence, so one {@code .*} silences the
 * whole catalog while every rule still reports green.
 *
 * <p><strong>Why this is not an ArchUnit rule.</strong> A detector that reported a violation would
 * be filtered by the very file it detects. {@link Detected} therefore throws straight out of
 * {@link ArchRule#check}: it builds no {@link EvaluationResult}, so nothing on the filtering path
 * ever sees it.
 *
 * <p><strong>Why it does not throw when the rule is built.</strong> {@code guard()} runs during a
 * rule class's static initialisation, and an exception there reaches the consumer as
 * {@code TestEngine with ID 'archunit' failed to discover tests} — the cause dropped, the message
 * gone. Deferring to evaluation makes it an ordinary test failure that prints in full.
 *
 * <p>Detection is deliberately not a log warning either: Corral ships no slf4j binding, so a warning
 * can surface for nobody — the same silence the check exists to remove.
 */
@UtilityClass
public class IgnorePatternsGuard {

    static final String IGNORE_PATTERNS_FILE = "archunit_ignore_patterns.txt";

    /**
     * Read with {@code getPropertyOrDefault}, so an absent {@code archunit.properties} and an absent
     * key both mean "fail". {@link ArchConfiguration} loads that file whole, without filtering by
     * prefix, so a {@code corral.}-prefixed key is readable there; only the <em>system property</em>
     * form needs ArchUnit's prefix ({@code -Darchunit.corral.ignorePatterns.fail=false}).
     */
    static final String FAIL_PROPERTY = "corral.ignorePatterns.fail";

    /**
     * Scanned once per JVM, when the first rule class runs {@code guard()}. Every later rule reads
     * the result, so the check costs one classpath walk however many rules a consumer evaluates.
     */
    private static final List<URL> LOCATIONS = locate(currentClassLoader());

    /**
     * {@code rule} unchanged, or — when the file is on the classpath and the check is armed — a rule
     * that fails with {@link #messageFor} instead of evaluating.
     */
    public static ArchRule interposeOn(ArchRule rule) {
        return interposeOn(rule, LOCATIONS);
    }

    static ArchRule interposeOn(ArchRule rule, List<URL> locations) {
        return locations.isEmpty() || !failEnabled()
                ? rule
                : new Detected(rule.getDescription(), messageFor(locations));
    }

    /**
     * Only the literal {@code false} disarms the check. A typo ({@code no}, {@code 0}, an empty
     * value) leaves it armed rather than quietly reproducing the failure mode it guards against.
     */
    private static boolean failEnabled() {
        return !"false".equalsIgnoreCase(ArchConfiguration.get().getPropertyOrDefault(FAIL_PROPERTY, "true"));
    }

    /**
     * {@code getResources}, plural. ArchUnit resolves the file with {@code getResource}, so the first
     * match wins and the copy doing the filtering need not be the one the consumer knows about — a
     * transitive test-scoped dependency shipping that filename disarms every rule downstream.
     */
    static List<URL> locate(ClassLoader classLoader) {
        try {
            return Collections.list(classLoader.getResources(IGNORE_PATTERNS_FILE));
        } catch (IOException e) {
            // A classpath that cannot be enumerated cannot be declared clean either.
            throw new UncheckedIOException("Could not scan the classpath for " + IGNORE_PATTERNS_FILE, e);
        }
    }

    /** Mirrors {@code ClassLoaders.getCurrentClassLoader}, which is how ArchUnit resolves the file. */
    static ClassLoader currentClassLoader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        return contextClassLoader != null ? contextClassLoader : IgnorePatternsGuard.class.getClassLoader();
    }

    static String messageFor(List<URL> locations) {
        return """
                %s is on the classpath. ArchUnit discards every violation matching a regex in that file \
                inside EvaluationResult's constructor — before FreezingArchRule, before the freeze store \
                and before any failure message — so a Corral rule silenced this way reports green and \
                leaves no record anywhere.
                Found (ArchUnit reads the first):
                %s
                Delete the file, or set %s=false in archunit.properties if it is yours and deliberate.
                Setting it to silence a rule you should have fixed restores the silence rather than \
                resolving it, and is the same move as deleting the test.\
                """.formatted(IGNORE_PATTERNS_FILE, listed(locations), FAIL_PROPERTY);
    }

    private static String listed(List<URL> locations) {
        return locations.stream().map(location -> "  " + location).collect(joining(System.lineSeparator()));
    }

    /**
     * The rule a consumer evaluates once the file is found: it keeps the id it replaced, so the
     * failure arrives under the test name they recognise, and it never evaluates anything — every
     * entry point throws the same message.
     */
    @RequiredArgsConstructor
    private static final class Detected implements ArchRule {

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
            return new Detected(newDescription, message);
        }

        /** Nothing a caller can chain may soften this into something that passes. */
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
