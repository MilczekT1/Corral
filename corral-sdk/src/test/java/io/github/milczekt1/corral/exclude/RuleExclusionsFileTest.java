package io.github.milczekt1.corral.exclude;

import static io.github.milczekt1.corral.exclude.RuleExclusions.EXCLUSIONS_FILE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.milczekt1.corral.exclude.RuleExclusions.Loaded;
import java.io.IOException;
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
 * The file half of {@link RuleExclusions}: what a line may say, and how the classpath is resolved.
 *
 * <p>Every parse failure is a {@link Loaded#problem()} rather than a thrown exception, because
 * loading happens in a static initialiser — an exception there reaches the consumer as
 * {@code failed to discover tests} with the cause dropped.
 */
class RuleExclusionsFileTest {

    private static final String SOURCE = "file:/somewhere/" + EXCLUSIONS_FILE;

    @Test
    void readsAnIdAndItsReason() {
        Loaded loaded = RuleExclusions.parse(
                "spring.no-transactional-on-final :: AspectJ load-time weaving. ADR-021.", SOURCE);

        assertNull(loaded.problem(), () -> "unexpected problem: " + loaded.problem());
        assertEquals(
                List.of(new Exclusion("spring.no-transactional-on-final",
                        "AspectJ load-time weaving. ADR-021.")),
                loaded.entries());
    }

    @Test
    void ignoresBlankLinesAndComments() {
        Loaded loaded = RuleExclusions.parse("""
                # <rule-id> :: <reason>

                   # indented comment, and the id may be indented too
                  logging.no-system-out   ::   stdout is our transport.\s\s

                """, SOURCE);

        assertNull(loaded.problem(), () -> "unexpected problem: " + loaded.problem());
        assertEquals(List.of(new Exclusion("logging.no-system-out", "stdout is our transport.")),
                loaded.entries());
    }

    @Test
    void keepsEveryLineInFileOrder() {
        Loaded loaded = RuleExclusions.parse("""
                logging.no-system-out :: first
                logging.no-system-err :: second
                """, SOURCE);

        assertEquals(List.of("logging.no-system-out", "logging.no-system-err"),
                loaded.entries().stream().map(Exclusion::ruleId).toList());
    }

    /** A reason is the one thing Corral cannot judge, so its absence is what it can make fatal. */
    @ParameterizedTest(name = "\"{0}\" is a parse error")
    @ValueSource(strings = {
            "logging.no-system-out",
            "logging.no-system-out ::",
            "logging.no-system-out ::   ",
            ":: no id at all",
            "   :: neither part",
    })
    void aLineWithoutBothAnIdAndAReasonIsAParseError(String line) {
        Loaded loaded = RuleExclusions.parse(line, SOURCE);

        assertNotNull(loaded.problem(), () -> "expected a parse error for: " + line);
        assertTrue(loaded.problem().contains(SOURCE), loaded::problem);
        assertTrue(loaded.entries().isEmpty(), () -> "a broken file must exclude nothing: " + loaded);
    }

    @Test
    void aParseErrorNamesTheLineNumberAndTheOffendingText() {
        Loaded loaded = RuleExclusions.parse("""
                # a comment

                logging.no-system-out :: fine
                logging.no-system-err
                """, SOURCE);

        assertNotNull(loaded.problem());
        assertTrue(loaded.problem().contains(":4"),
                () -> "must name line 4, the offending line: " + loaded.problem());
        assertTrue(loaded.problem().contains("logging.no-system-err"), loaded::problem);
    }

    /** Reporting only the first would turn fixing the file into a round trip per line. */
    @Test
    void everyBrokenLineIsReportedNotOnlyTheFirst() {
        Loaded loaded = RuleExclusions.parse("""
                first.no-reason
                second.no-reason
                """, SOURCE);

        assertTrue(loaded.problem().contains("first.no-reason"), loaded::problem);
        assertTrue(loaded.problem().contains("second.no-reason"), loaded::problem);
    }

    /** The id is a freeze-store key with a fixed shape; prose in that position is a typo, not an id. */
    @ParameterizedTest(name = "\"{0}\" is not the shape of a rule id")
    @ValueSource(strings = {
            "Logging.No-System-Out",
            "logging",
            "logging.no system out",
            "logging.no-system-out.",
    })
    void anIdThatIsNotTheShapeOfARuleIdIsAParseError(String id) {
        Loaded loaded = RuleExclusions.parse(id + " :: a reason", SOURCE);

        assertNotNull(loaded.problem(), () -> "expected a parse error for id: " + id);
        assertTrue(loaded.problem().contains(id), loaded::problem);
    }

    /** Two reasons for one exclusion is a merge accident; which one is in effect is unanswerable. */
    @Test
    void thesameIdExcludedTwiceIsAParseError() {
        Loaded loaded = RuleExclusions.parse("""
                logging.no-system-out :: our transport is stdout
                logging.no-system-out :: and also this
                """, SOURCE);

        assertNotNull(loaded.problem());
        assertTrue(loaded.problem().contains("logging.no-system-out"), loaded::problem);
    }

    @Test
    void aFileOfNothingButCommentsExcludesNothingAndIsNotAnError() {
        Loaded loaded = RuleExclusions.parse("# nothing to say yet\n", SOURCE);

        assertNull(loaded.problem());
        assertTrue(loaded.entries().isEmpty());
    }

    /** The record is the invariant: an Exclusion that exists is well formed. */
    @Test
    void anExclusionCannotBeBuiltWithoutAWellFormedIdAndAReason() {
        assertThrows(IllegalArgumentException.class, () -> new Exclusion(null, "a reason"));
        assertThrows(IllegalArgumentException.class, () -> new Exclusion("logging.no-system-out", null));
        assertThrows(IllegalArgumentException.class, () -> new Exclusion("logging.no-system-out", "  "));
        assertThrows(IllegalArgumentException.class, () -> new Exclusion("Not An Id", "a reason"));
    }

    @Test
    void noFileOnTheClassPathMeansNoExclusionsAndNoProblem(@TempDir Path emptyRoot) throws IOException {
        try (URLClassLoader classLoader = classLoaderOver(emptyRoot)) {
            Loaded loaded = RuleExclusions.load(classLoader);

            assertNull(loaded.problem());
            assertTrue(loaded.entries().isEmpty());
        }
    }

    @Test
    void theFileIsReadFromTheClassPath(@TempDir Path root) throws IOException {
        writeExclusionsIn(root, "logging.no-system-out :: our transport is stdout");

        try (URLClassLoader classLoader = classLoaderOver(root)) {
            Loaded loaded = RuleExclusions.load(classLoader);

            assertNull(loaded.problem(), loaded::problem);
            assertEquals(List.of(new Exclusion("logging.no-system-out", "our transport is stdout")),
                    loaded.entries());
        }
    }

    /**
     * A second copy would otherwise be resolved by first-match-wins, and the copy doing the excluding
     * could be one a transitive test-scoped dependency shipped rather than one the consumer wrote.
     */
    @Test
    void moreThanOneCopyOnTheClassPathIsAProblemThatNamesEveryLocation(
            @TempDir Path first, @TempDir Path second) throws IOException {
        Path firstCopy = writeExclusionsIn(first, "logging.no-system-out :: mine");
        Path secondCopy = writeExclusionsIn(second, "logging.no-system-err :: someone else's");

        try (URLClassLoader classLoader = classLoaderOver(first, second)) {
            Loaded loaded = RuleExclusions.load(classLoader);

            assertNotNull(loaded.problem());
            assertTrue(loaded.problem().contains(firstCopy.toString()), loaded::problem);
            assertTrue(loaded.problem().contains(secondCopy.toString()), loaded::problem);
            assertTrue(loaded.entries().isEmpty(),
                    () -> "an ambiguous file must exclude nothing: " + loaded);
        }
    }

    /**
     * {@code load} runs in a static initialiser, so anything it lets escape becomes an
     * {@code ExceptionInInitializerError} — an {@link Error}, which sails past every
     * {@code catch (RuntimeException)} guarding the formatter's never-throw contract, and leaves
     * {@code NoClassDefFoundError} behind on every later call. A URL handler that throws unchecked
     * is the realistic route in.
     */
    @Test
    void anUncheckedFailureWhileReadingIsAProblemRatherThanEscaping() {
        ClassLoader throwsUnchecked = new ClassLoader(null) {
            @Override
            public Enumeration<URL> getResources(String name) {
                throw new IllegalStateException("a URL handler misbehaved");
            }
        };

        Loaded loaded = assertDoesNotThrow(() -> RuleExclusions.load(throwsUnchecked));

        assertNotNull(loaded.problem());
        assertTrue(loaded.problem().contains(EXCLUSIONS_FILE), loaded::problem);
        assertTrue(loaded.entries().isEmpty());
    }

    /** A classpath that cannot be enumerated cannot be declared free of exclusions either. */
    @Test
    void anUnreadableClassPathIsAProblemRatherThanNoExclusions() {
        ClassLoader unreadable = new ClassLoader(null) {
            @Override
            public Enumeration<URL> getResources(String name) throws IOException {
                throw new IOException("classpath unreadable");
            }
        };

        Loaded loaded = RuleExclusions.load(unreadable);

        assertNotNull(loaded.problem());
        assertTrue(loaded.problem().contains(EXCLUSIONS_FILE), loaded::problem);
    }

    /** The absent direction on this build's own classpath, through the memoised scan. */
    @Test
    void thisBuildCarriesNoExclusions() {
        assertTrue(RuleExclusions.inEffect().isEmpty(),
                "this module must not commit an exclusions file, or every test here proves less");
        assertFalse(RuleExclusions.isBroken());
    }

    private static Path writeExclusionsIn(Path root, String contents) throws IOException {
        return Files.writeString(root.resolve(EXCLUSIONS_FILE), contents + System.lineSeparator());
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
