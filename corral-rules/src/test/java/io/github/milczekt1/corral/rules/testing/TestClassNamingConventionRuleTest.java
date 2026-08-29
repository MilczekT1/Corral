package io.github.milczekt1.corral.rules.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TestClassNamingConventionRuleTest {

    private static final JavaClasses FIXTURES = new ClassFileImporter()
            .importPackages("io.github.milczekt1.corral.fixtures.testing");

    /** The JUnit 5 roots the rule is configured with. */
    private static final List<String> EXPECTED_JUNIT_ROOTS = List.of(
            "org.junit.jupiter.api.Test",
            "org.junit.jupiter.api.TestFactory",
            "org.junit.jupiter.api.TestTemplate");

    private static String report(ArchRule rule) {
        return String.join("\n", rule.allowEmptyShould(true).evaluate(FIXTURES).getFailureReport().getDetails());
    }

    /**
     * Every class Surefire would never run, whatever route JUnit takes to its test methods: a plain
     * {@code @Test}, a {@code @ParameterizedTest} with no {@code @Test} anywhere, and a consumer's
     * own {@code @FastTest} composed annotation.
     */
    @ParameterizedTest(name = "flags {0}")
    @ValueSource(strings = {"BadlyNamedTestCase", "BadlyNamedParameterizedCase", "BadlyNamedComposedCase"})
    void flagsTestClassesThatSurefireWouldNeverRun(String fixture) {
        String report = report(TestClassNamingConventionRule.DEFINITION);

        assertTrue(report.contains(fixture), report);
    }

    @Test
    void acceptsConventionallyNamedTestClasses() {
        String report = report(TestClassNamingConventionRule.DEFINITION);

        assertFalse(report.contains("WellNamedTest"), report);
        assertFalse(report.contains("PlainUnitTest"), report);
    }

    @Test
    void staysSilentOnJunit5NestedGroups() {
        // A @Nested class is its own JavaClass, but Surefire only ever selects the enclosing one.
        String report = report(TestClassNamingConventionRule.DEFINITION);

        assertFalse(report.contains("WhenEmpty"), report);
        assertFalse(report.contains("WhenPopulated"), report);
        assertFalse(report.contains("NestedGroupsTest"), report);
    }

    @Test
    void staysSilentOnClassesWithNoTestMethodsAtAll() {
        // The mock fixtures declare fields only, so no naming verdict applies.
        String report = report(TestClassNamingConventionRule.DEFINITION);

        assertFalse(report.contains("MockingRepositoryIT"), report);
        assertFalse(report.contains("MockingDaoIT"), report);
        assertFalse(report.contains("MockingGatewayIT"), report);
        assertFalse(report.contains("OrderRepository"), report);
        assertFalse(report.contains("PaymentGateway"), report);
    }

    @Test
    void pinsTheJunit5RootAnnotations() {
        // Roots only: @ParameterizedTest and @RepeatedTest are reached through @TestTemplate.
        assertEquals(EXPECTED_JUNIT_ROOTS, TestClassNamingConventionRule.JUNIT_TEST_ANNOTATIONS);
    }

    @Test
    void publicRuleIsFrozenAndIdPinned() {
        assertEquals("corral.test.class-names-must-end-with-test-or-it", TestClassNamingConventionRule.rule.getDescription());
    }
}
