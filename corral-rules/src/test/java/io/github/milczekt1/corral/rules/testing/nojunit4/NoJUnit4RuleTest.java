package io.github.milczekt1.corral.rules.testing.nojunit4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.scope.TestScope;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import junit.extensions.TestSetup;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.junit.Before;
import org.junit.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.annotation.Testable;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * The examples are nested and static, so neither Surefire nor Jupiter selects them — Surefire matches
 * class files ending in {@code Test}/{@code Tests}/{@code TestCase}, and Jupiter descends only into
 * {@code @Nested} inner classes. Their JUnit 4 annotations are therefore compiled and never executed,
 * which is exactly the silent state this rule exists to find.
 *
 * <p>{@code org.junit.Test} is written out in full where it is needed: importing it would collide with
 * {@code org.junit.jupiter.api.Test}, which is the one-segment confusion this rule is about.
 */
class NoJUnit4RuleTest {

    /**
     * MUST FLAG on {@code @Before} only. The Jupiter {@code @Test} in the same class is the
     * must-not-match half: an over-broad predicate that dropped the Jupiter exclusion finds it too.
     */
    static class HalfMigratedCase {

        @Before
        void seedTheOrder() {
        }

        @Test
        void placesTheOrder() {
        }
    }

    /** MUST FLAG on {@code org.junit.Assert} only — the Jupiter assertion beside it must stay silent. */
    static class MessageFirstAssertCase {

        @Test
        void comparesTwoWays() {
            org.junit.Assert.assertEquals("message first, JUnit 4", 1, 1);
            Assertions.assertEquals(1, 1, "message last, Jupiter");
        }
    }

    /**
     * MUST FLAG: declares no test of its own, so a rule scoped to classes-declaring-a-test would report
     * nothing while the fixture silently never ran for any Jupiter subclass extending this.
     */
    abstract static class AbstractSeededBase {

        @Before
        void seedTheDatabase() {
        }
    }

    /**
     * MUST FLAG: {@code org.junit.runner} and {@code org.junit.rules} are subpackages, so this pins the
     * trailing {@code ..} in {@code org.junit..} — without it neither is matched.
     */
    @RunWith(JUnit4.class)
    static class RunnerAndRuleCase {

        @Rule
        public TemporaryFolder temporaryFolder = new TemporaryFolder();

        @org.junit.Test
        void writesAFile() {
        }
    }

    /** MUST FLAG: JUnit 3, reached by inheritance rather than by an annotation. */
    static class JUnit3StyleCase extends TestCase {

        public void testTheTotal() {
            assertEquals("inherited from junit.framework.Assert", 1, 1);
        }
    }

    /** MUST FLAG: the JUnit 3 decorator package, which ships in junit:junit alongside the rest. */
    static class SuiteDecoratorCase {

        TestSetup decorateWithOneTimeSetup(TestCase testCase) {
            return new TestSetup(testCase);
        }
    }

    /** MUST IGNORE: Jupiter, its params module and the Platform are all separately excluded packages. */
    @Testable
    static class JupiterOnlyCase {

        @Test
        void chargesTheCard() {
            Assertions.assertEquals(1, 1, "card should be charged");
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 2})
        void chargesEachCard(int amount) {
            Assertions.assertTrue(amount > 0);
        }
    }

    /** MUST IGNORE: an {@code assertEquals} of its own is not a dependency on anyone else's. */
    static class HomegrownAssertCase {

        static void assertEquals(long expected, long actual) {
            if (expected != actual) {
                throw new AssertionError(expected + " != " + actual);
            }
        }

        void checksTheTotal() {
            assertEquals(1, 1);
        }
    }

    private static final String ID = "corral.test.no-junit4";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    /**
     * {@link TestSuite} is JUnit 4's own code, loaded from a jar rather than test output, so it pins
     * the scope clause: it is soaked in JUnit 3 types and must still not be reported.
     */
    private static final JavaClasses EXAMPLES = new ClassFileImporter().importClasses(
            HalfMigratedCase.class, MessageFirstAssertCase.class, AbstractSeededBase.class,
            RunnerAndRuleCase.class, JUnit3StyleCase.class, SuiteDecoratorCase.class,
            JupiterOnlyCase.class, HomegrownAssertCase.class, TestSuite.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static String report() {
        return String.join("\n", NoJUnit4Rule.DEFINITION
                .allowEmptyShould(true).evaluate(EXAMPLES).getFailureReport().getDetails());
    }

    @Test
    void flagsAJUnit4FixtureAnnotation() {
        String report = report();

        assertTrue(report.contains("HalfMigratedCase"), report);
        assertTrue(report.contains("org.junit.Before"), report);
    }

    @Test
    void ignoresJupiterTypesInTheSameClasses() {
        String report = report();

        assertFalse(report.contains("org.junit.jupiter"),
                "Jupiter is the destination of the migration, not a violation of it: " + report);
    }

    @Test
    void flagsAJUnit4AssertionCall() {
        String report = report();

        assertTrue(report.contains("MessageFirstAssertCase"), report);
        assertTrue(report.contains("org.junit.Assert"), report);
    }

    @Test
    void flagsAJUnit4FixtureOnAClassThatDeclaresNoTest() {
        String report = report();

        assertTrue(report.contains("AbstractSeededBase"),
                "the base class is where a fixture that never runs can be caught: " + report);
    }

    @Test
    void flagsRunnersAndRulesInSubpackages() {
        String report = report();

        assertTrue(report.contains("org.junit.runner.RunWith"), report);
        assertTrue(report.contains("org.junit.rules.TemporaryFolder"), report);
    }

    @Test
    void flagsAJUnit3TestCaseSubclass() {
        String report = report();

        assertTrue(report.contains("JUnit3StyleCase"), report);
        assertTrue(report.contains("junit.framework."), report);
    }

    @Test
    void flagsTheJUnit3ExtensionsPackage() {
        String report = report();

        assertTrue(report.contains("junit.extensions.TestSetup"), report);
    }

    @Test
    void ignoresATestUsingOnlyJupiterAndThePlatform() {
        String report = report();

        assertFalse(report.contains("JupiterOnlyCase"),
                "Jupiter, junit-jupiter-params and the Platform are all excluded: " + report);
    }

    @Test
    void ignoresAProjectsOwnAssertEquals() {
        String report = report();

        assertFalse(report.contains("HomegrownAssertCase"),
                "the name is JUnit 4's, the type is not: " + report);
    }

    /**
     * Vintage is the one excluded package with no flagged example, so it is pinned here instead:
     * putting junit-vintage-engine on this classpath to write one would make every JUnit 4 example
     * above actually execute, which is the arrangement the rule exists to prevent.
     */
    @Test
    void excludesEveryPackageUnderOrgJunitThatJUnit4DoesNotOwn() {
        assertEquals(List.of("org.junit.jupiter..", "org.junit.platform..", "org.junit.vintage.."),
                NoJUnit4Rule.NOT_JUNIT4_PACKAGES);
    }

    /** Premises asserted, so a JUnit 4 that changed fails loudly instead of unpinning the clause. */
    @Test
    void ignoresJUnit4TypesOutsideTestOutput() {
        JavaClass testSuite = EXAMPLES.get(TestSuite.class);

        assertFalse(TestScope.TEST_CLASSES.test(testSuite),
                "premise: a jar-sourced class is in no test output directory, so it is production-scoped");
        assertTrue(testSuite.getDirectDependenciesFromSelf().stream()
                        .anyMatch(dependency -> dependency.getTargetClass().getPackageName()
                                .startsWith("junit.framework")),
                "premise: TestSuite must still depend on junit.framework types, or this pins nothing");

        assertFalse(report().contains("Class <junit.framework.TestSuite>"),
                "this rule is scoped to test classes, and JUnit 4's own code is not one: " + report());
    }

    /**
     * {@link FreezingArchRule#persistIn}, not {@code freeze.store}: a frozen rule captures its store
     * when constructed, which is class initialisation, so naming one here races class loading. Only
     * the path goes on the process-wide {@link ArchConfiguration}.
     *
     * <p>Reseed with {@code -Darchunit.freeze.store.default.allowStoreCreation=true}, then commit.
     */
    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        ArchConfiguration.get().setProperty("freeze.store.default.path", STORE_PATH);
        try {
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoJUnit4Rule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(EXAMPLES);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("HalfMigratedCase"), debt);
            assertFalse(debt.contains("JupiterOnlyCase"),
                    "an allowed dependency must never reach the store: " + debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
