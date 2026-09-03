package io.github.milczekt1.corral.rules.testing.testclassnamingconvention;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The examples are top-level classes in this rule's own {@code fixtures} package, not nested classes
 * as elsewhere in the catalog: this rule's predicate ends at {@code areNotMemberClasses()}, so a
 * nested example is exempt by construction and could never be flagged. The {@code fixtures} path
 * segment is what keeps {@code BadlyNamedTestCase} and {@code ConventionalIntegrationIT} from being
 * selected as real tests — Surefire and Failsafe both exclude it.
 */
class TestClassNamingConventionRuleTest {

    private static final String ID = "corral.test.class-names-must-end-with-test-or-it";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

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
     * Every class a build tool would never run, whatever route JUnit takes to its test methods: a
     * plain {@code @Test}, a {@code @ParameterizedTest} with no {@code @Test} anywhere, and a
     * consumer's own {@code @FastTest} composed annotation.
     */
    @ParameterizedTest(name = "flags {0}")
    @ValueSource(strings = {"BadlyNamedTestCase", "BadlyNamedParameterizedCase", "BadlyNamedComposedCase"})
    void flagsTestClassesThatSurefireWouldNeverRun(String example) {
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
        // A @Nested class is its own JavaClass, but a build tool only ever selects the enclosing one.
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, TestClassNamingConventionRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(EXAMPLES);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("BadlyNamedTestCase"), debt);
            assertFalse(debt.contains("ConventionalUnitTest"),
                    "an accepted class must never reach the store: " + debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
