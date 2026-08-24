package io.github.milczekt1.corral.guard;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static io.github.milczekt1.corral.guard.IgnorePatternsGuard.FAIL_PROPERTY;
import static io.github.milczekt1.corral.guard.IgnorePatternsGuard.IGNORE_PATTERNS_FILE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * This JVM carries no {@code archunit_ignore_patterns.txt}, so the "present" case uses real files in
 * temp directories behind a real {@link URLClassLoader}. Both directions asserted — the absent one
 * alone passes vacuously.
 */
class IgnorePatternsGuardTest {

    private static final ArchRule INNOCENT_RULE =
            noClasses().should().accessField(System.class, "in").as("fixture.ignore-patterns-guard");

    @Test
    void findsNothingWhenTheFileIsNotOnTheClassPath(@TempDir Path emptyRoot) throws IOException {
        try (URLClassLoader classLoader = classLoaderOver(emptyRoot)) {
            assertEquals(List.of(), IgnorePatternsGuard.locate(classLoader));
        }
    }

    @Test
    void leavesTheRuleUntouchedWhenTheFileIsNotOnTheClassPath(@TempDir Path emptyRoot) throws IOException {
        try (URLClassLoader classLoader = classLoaderOver(emptyRoot)) {
            List<URL> found = IgnorePatternsGuard.locate(classLoader);

            assertSame(INNOCENT_RULE, IgnorePatternsGuard.interposeOn(INNOCENT_RULE, found));
        }
    }

    /** First match wins, so reporting only it would hide the copy worth knowing about. */
    @Test
    void findsEveryCopyOnTheClassPathNotOnlyTheOneArchUnitReads(@TempDir Path first, @TempDir Path second)
            throws IOException {
        Path firstCopy = writeIgnorePatternsIn(first);
        Path secondCopy = writeIgnorePatternsIn(second);

        try (URLClassLoader classLoader = classLoaderOver(first, second)) {
            List<String> found = locationsAsText(IgnorePatternsGuard.locate(classLoader));

            assertEquals(2, found.size(), () -> "expected both copies, got " + found);
            assertTrue(found.get(0).endsWith(firstCopy.toString()), found::toString);
            assertTrue(found.get(1).endsWith(secondCopy.toString()), found::toString);
        }
    }

    @Test
    void failsNamingTheFileEveryLocationAndTheProperty(@TempDir Path first, @TempDir Path second)
            throws IOException {
        writeIgnorePatternsIn(first);
        writeIgnorePatternsIn(second);

        try (URLClassLoader classLoader = classLoaderOver(first, second)) {
            List<URL> found = IgnorePatternsGuard.locate(classLoader);

            String message = failureOf(IgnorePatternsGuard.interposeOn(INNOCENT_RULE, found));

            assertTrue(message.contains(IGNORE_PATTERNS_FILE), message);
            assertTrue(message.contains(FAIL_PROPERTY),
                    () -> "a failure that does not name the way out is a dead end: " + message);
            found.forEach(location -> assertTrue(message.contains(location.toString()), message));
        }
    }

    /** Keeps the id, so the failure arrives under a recognisable test name; nothing chained softens it. */
    @Test
    void theReplacementKeepsTheRuleIdAndCannotBeChainedIntoPassing(@TempDir Path root) throws IOException {
        ArchRule detected = IgnorePatternsGuard.interposeOn(INNOCENT_RULE, locateIn(root));

        JavaClasses none = nothingToMatch();
        ArchRule because = detected.because("we need it");
        ArchRule allowingEmpty = detected.allowEmptyShould(true);

        assertNotSame(INNOCENT_RULE, detected);
        assertEquals(INNOCENT_RULE.getDescription(), detected.getDescription());
        assertEquals("renamed", detected.as("renamed").getDescription());
        assertThrows(AssertionError.class, () -> because.check(none));
        assertThrows(AssertionError.class, () -> allowingEmpty.check(none));
        assertThrows(AssertionError.class, () -> detected.evaluate(none));
    }

    /** {@code withThreadLocalScope}: config is process-wide, Surefire reuses the JVM, and it cannot be unset. */
    @ParameterizedTest(name = "\"{0}\" disarms the check")
    @ValueSource(strings = {"false", "FALSE", "False"})
    void theFailPropertySetToFalseSuppressesTheFailure(String value, @TempDir Path root) throws IOException {
        List<URL> found = locateIn(root);

        ArchConfiguration.withThreadLocalScope(configuration -> {
            configuration.setProperty(FAIL_PROPERTY, value);
            assertSame(INNOCENT_RULE, IgnorePatternsGuard.interposeOn(INNOCENT_RULE, found));
        });
    }

    /** A typo must not silently disarm the catalog's most complete kill switch. */
    @ParameterizedTest(name = "\"{0}\" leaves the check armed")
    @ValueSource(strings = {"true", "no", "0", ""})
    void anythingButFalseLeavesTheCheckArmed(String value, @TempDir Path root) throws IOException {
        List<URL> found = locateIn(root);

        ArchConfiguration.withThreadLocalScope(configuration -> {
            configuration.setProperty(FAIL_PROPERTY, value);
            assertNotSame(INNOCENT_RULE, IgnorePatternsGuard.interposeOn(INNOCENT_RULE, found));
        });
    }

    /** A consumer who configured nothing gets the failing behaviour. */
    @Test
    void failsByDefaultWhenThePropertyIsNotConfigured(@TempDir Path root) throws IOException {
        assertEquals("unset", ArchConfiguration.get().getPropertyOrDefault(FAIL_PROPERTY, "unset"),
                "this module must not configure the property, or the assertion below proves nothing");
        ArchRule detected = IgnorePatternsGuard.interposeOn(INNOCENT_RULE, locateIn(root));
        JavaClasses none = nothingToMatch();

        assertThrows(AssertionError.class, () -> detected.check(none));
    }

    /** The absent direction end to end, through the memoised scan of this build's real classpath. */
    @Test
    void leavesRulesAloneOnThisBuildsOwnClassPath() {
        assertSame(INNOCENT_RULE, IgnorePatternsGuard.interposeOn(INNOCENT_RULE));
    }

    /** An unreadable classpath must not report clean — that is the silent pass this check removes. */
    @Test
    void anUnreadableClassPathFailsRatherThanReportingClean() {
        ClassLoader unreadable = new ClassLoader(null) {
            @Override
            public Enumeration<URL> getResources(String name) throws IOException {
                throw new IOException("classpath unreadable");
            }
        };

        assertThrows(UncheckedIOException.class, () -> IgnorePatternsGuard.locate(unreadable));
    }

    /** ArchUnit falls back to its own loader when a thread has no context loader; so must this. */
    @Test
    void resolvesALoaderWhetherOrNotTheThreadHasAContextClassLoader() {
        Thread current = Thread.currentThread();
        ClassLoader contextClassLoader = current.getContextClassLoader();
        assertSame(contextClassLoader, IgnorePatternsGuard.currentClassLoader());

        current.setContextClassLoader(null);
        try {
            assertSame(IgnorePatternsGuard.class.getClassLoader(), IgnorePatternsGuard.currentClassLoader());
        } finally {
            current.setContextClassLoader(contextClassLoader);
        }
    }

    private static String failureOf(ArchRule rule) {
        JavaClasses none = nothingToMatch();
        return assertThrows(AssertionError.class, () -> rule.check(none)).getMessage();
    }

    /** No classes at all: the replacement must fail before it can care what it was handed. */
    private static JavaClasses nothingToMatch() {
        return new ClassFileImporter().importClasses();
    }

    private static List<URL> locateIn(Path root) throws IOException {
        writeIgnorePatternsIn(root);
        try (URLClassLoader classLoader = classLoaderOver(root)) {
            return IgnorePatternsGuard.locate(classLoader);
        }
    }

    /** {@link URL#equals} resolves hosts, so locations are compared as text. */
    private static List<String> locationsAsText(List<URL> locations) {
        return locations.stream().map(URL::toString).toList();
    }

    /** A pattern that would suppress every violation of every rule — the case that matters most. */
    private static Path writeIgnorePatternsIn(Path root) throws IOException {
        return Files.writeString(root.resolve(IGNORE_PATTERNS_FILE), ".*" + System.lineSeparator());
    }

    /** No parent, so only the temporary directories answer — this build's own classpath cannot leak in. */
    private static URLClassLoader classLoaderOver(Path... roots) throws IOException {
        URL[] urls = new URL[roots.length];
        for (int i = 0; i < roots.length; i++) {
            urls[i] = roots[i].toUri().toURL();
        }
        return new URLClassLoader(urls, null);
    }
}
