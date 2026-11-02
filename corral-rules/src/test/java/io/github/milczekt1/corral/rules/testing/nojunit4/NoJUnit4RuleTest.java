package io.github.milczekt1.corral.rules.testing.nojunit4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.testing.nojunit4.fixtures.AbstractSeededBase;
import io.github.milczekt1.corral.rules.testing.nojunit4.fixtures.HalfMigratedCase;
import io.github.milczekt1.corral.rules.testing.nojunit4.fixtures.HomegrownAssertCase;
import io.github.milczekt1.corral.rules.testing.nojunit4.fixtures.JUnit3StyleCase;
import io.github.milczekt1.corral.rules.testing.nojunit4.fixtures.JupiterOnlyCase;
import io.github.milczekt1.corral.rules.testing.nojunit4.fixtures.MessageFirstAssertCase;
import io.github.milczekt1.corral.rules.testing.nojunit4.fixtures.RunnerAndRuleCase;
import io.github.milczekt1.corral.rules.testing.nojunit4.fixtures.SuiteDecoratorCase;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import io.github.milczekt1.corral.scope.TestScope;
import java.io.IOException;
import java.util.List;
import junit.framework.TestSuite;
import org.junit.jupiter.api.Test;

/**
 * The examples live in {@code fixtures/}, which Surefire and Sonar both exclude: they are
 * deliberately bad code, and a linter told to fix any of it would delete the violation under test.
 */
class NoJUnit4RuleTest {

    private static final String ID = "corral.test.no-junit4";

    /**
     * {@link TestSuite} is JUnit 4's own code, loaded from a jar rather than test output: it pins the
     * scope clause.
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
     * Vintage has no flagged example: junit-vintage-engine on this classpath would make every JUnit 4
     * example actually execute.
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

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoJUnit4Rule.rule, ID, EXAMPLES);

        assertTrue(debt.contains("HalfMigratedCase"), debt);
        assertFalse(debt.contains("JupiterOnlyCase"),
                "an allowed dependency must never reach the store: " + debt);
    }
}
