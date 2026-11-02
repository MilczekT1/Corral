package io.github.milczekt1.corral.rules.testing.testclassnamingconvention;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import io.github.milczekt1.corral.rules.testsupport.FixtureVerdicts;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The examples are top-level classes in this rule's own {@code fixtures} package, not nested classes
 * as elsewhere in the catalog: this rule's predicate ends at {@code areNotMemberClasses()}, so a
 * nested example is exempt by construction and could never be flagged. The {@code fixtures} path
 * segment is what keeps the conventionally named examples — {@code ConventionalUnitTest} and
 * {@code ConventionalIntegrationIT} among them — from being selected as real tests here: Surefire
 * and Failsafe both exclude that segment.
 */
class TestClassNamingConventionRuleTest {

    private static final String ID = "corral.test.class-names-must-end-with-test-or-it";

    private static final JavaClasses EXAMPLES = new ClassFileImporter()
            .importPackages(TestClassNamingConventionRuleTest.class.getPackageName() + ".fixtures");

    /** The JUnit 5 roots the rule is configured with. */
    private static final List<String> EXPECTED_JUNIT_ROOTS = List.of(
            "org.junit.jupiter.api.Test",
            "org.junit.jupiter.api.TestFactory",
            "org.junit.jupiter.api.TestTemplate");

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static String report() {
        return String.join("\n", TestClassNamingConventionRule.DEFINITION
                .allowEmptyShould(true).evaluate(EXAMPLES).getFailureReport().getDetails());
    }

    /**
     * Every class a name-based selection convention passes over, whatever route JUnit takes to its
     * test methods: a plain {@code @Test}, a {@code @ParameterizedTest} with no {@code @Test}
     * anywhere, and a consumer's own {@code @FastTest} composed annotation.
     */
    @ParameterizedTest(name = "flags {0}")
    @ValueSource(strings = {"BadlyNamedPlainCase", "BadlyNamedParameterizedCase", "BadlyNamedComposedCase"})
    void flagsTestClassesNoNameConventionSelects(String example) {
        String report = report();

        assertTrue(report.contains(example), report);
    }

    /** One example per accepted suffix, so deleting any of the three clauses fails here. */
    @ParameterizedTest(name = "accepts {0}")
    @ValueSource(strings = {"ConventionalUnitTest", "ConventionalGroupedTests", "ConventionalIntegrationIT"})
    void acceptsEachConventionalSuffix(String example) {
        String report = report();

        assertFalse(report.contains(example), report);
    }

    @Test
    void staysSilentOnJunit5NestedGroups() {
        // A @Nested class is its own JavaClass, but selection only ever reads the enclosing name.
        String report = report();

        assertFalse(report.contains("WhenEmpty"), report);
        assertFalse(report.contains("WhenPopulated"), report);
    }

    @Test
    void staysSilentOnAnUnconventionallyNamedClassWithNoTestMethods() {
        String report = report();

        assertFalse(report.contains("HelperWithoutTestMethods"),
                "a class holding no JUnit test method gets no naming verdict: " + report);
    }

    @Test
    void pinsTheJunit5RootAnnotations() {
        // Roots only: @ParameterizedTest and @RepeatedTest are reached through @TestTemplate.
        assertEquals(EXPECTED_JUNIT_ROOTS, TestClassNamingConventionRule.JUNIT_TEST_ANNOTATIONS);
    }

    @Test
    void everyFixtureMatchesItsVerdict() {
        FixtureVerdicts.assertMatch(TestClassNamingConventionRuleTest.class, TestClassNamingConventionRule.DEFINITION, EXAMPLES);
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(TestClassNamingConventionRule.rule, ID, EXAMPLES);

        assertTrue(debt.contains("BadlyNamedPlainCase"), debt);
        assertFalse(debt.contains("ConventionalUnitTest"),
                "an accepted class must never reach the store: " + debt);
    }
}
