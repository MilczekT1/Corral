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
 * Fails the build when {@value #IGNORE_PATTERNS_FILE} is on the classpath. ArchUnit reads that file
 * in {@code EvaluationResult}'s constructor and discards matching violations before
 * {@code FreezingArchRule}, the freeze store or any failure message sees them, so one {@code .*}
 * silences the catalog while every rule reports green.
 *
 * <p>{@link Detected} throws from {@link ArchRule#check}: a violation would be filtered by the very
 * file it reports, and a throw from {@code guard()} reaches the consumer as
 * {@code failed to discover tests} with the cause dropped.
 */
@UtilityClass
public class IgnorePatternsGuard {

    static final String IGNORE_PATTERNS_FILE = "archunit_ignore_patterns.txt";

    /**
     * Absent file and absent key both mean "fail". Readable from {@code archunit.properties} despite
     * the prefix; only the system-property form needs ArchUnit's
     * ({@code -Darchunit.corral.ignorePatterns.fail=false}).
     */
    static final String FAIL_PROPERTY = "corral.ignorePatterns.fail";

    /** Scanned once per JVM, on the first {@code guard()} call — one classpath walk for any number of rules. */
    private static final List<URL> LOCATIONS = locate(currentClassLoader());

    /** {@code rule} unchanged, or one that fails with {@link #messageFor} when the file is found. */
    public static ArchRule interposeOn(ArchRule rule) {
        return interposeOn(rule, LOCATIONS);
    }

    static ArchRule interposeOn(ArchRule rule, List<URL> locations) {
        return locations.isEmpty() || !failEnabled()
                ? rule
                : new Detected(rule.getDescription(), messageFor(locations));
    }

    /** Only the literal {@code false} disarms it — a typo leaves it armed rather than silently open. */
    private static boolean failEnabled() {
        return !"false".equalsIgnoreCase(ArchConfiguration.get().getPropertyOrDefault(FAIL_PROPERTY, "true"));
    }

    /**
     * Plural: ArchUnit uses {@code getResource}, so first match wins and the copy doing the filtering
     * may be one a transitive dependency shipped, not one the consumer put there.
     */
    static List<URL> locate(ClassLoader classLoader) {
        try {
            return Collections.list(classLoader.getResources(IGNORE_PATTERNS_FILE));
        } catch (IOException e) {
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

    /** Keeps the id it replaced, so the failure arrives under a recognisable test name. */
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
