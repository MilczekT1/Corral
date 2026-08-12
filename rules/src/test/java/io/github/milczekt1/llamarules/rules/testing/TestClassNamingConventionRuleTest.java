package io.github.milczekt1.llamarules.rules.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class TestClassNamingConventionRuleTest {

    private static final JavaClasses FIXTURES = new ClassFileImporter()
            .importPackages("io.github.milczekt1.llamarules.fixtures.testing");

    private static String report(ArchRule rule) {
        return String.join("\n", rule.allowEmptyShould(true).evaluate(FIXTURES).getFailureReport().getDetails());
    }

    @Test
    void flagsTestClassesThatSurefireWouldNeverRun() {
        String report = report(TestClassNamingConventionRule.RULE);

        assertTrue(report.contains("BadlyNamedTestCase"), report);
    }

    @Test
    void flagsClassesWhoseOnlyTestsAreParameterized() {
        // The false negative the rule's own `why` text describes: no @Test anywhere, so the old
        // @Test-only predicate never selected the class and Surefire never ran it either.
        String report = report(TestClassNamingConventionRule.RULE);

        assertTrue(report.contains("BadlyNamedParameterizedCase"), report);
    }

    @Test
    void acceptsConventionallyNamedTestClasses() {
        String report = report(TestClassNamingConventionRule.RULE);

        assertFalse(report.contains("WellNamedTest"), report);
        assertFalse(report.contains("PlainUnitTest"), report);
    }

    @Test
    void staysSilentOnJunit5NestedGroups() {
        // A @Nested class is imported as its own JavaClass named e.g. WhenEmpty and holds @Test
        // methods, but Surefire only ever selects the enclosing class — flagging it would be a
        // false positive whose only "fix" is a rename that changes nothing.
        String report = report(TestClassNamingConventionRule.RULE);

        assertFalse(report.contains("WhenEmpty"), report);
        assertFalse(report.contains("WhenPopulated"), report);
        assertFalse(report.contains("NestedGroupsTest"), report);
    }

    @Test
    void staysSilentOnClassesWithNoTestMethodsAtAll() {
        // The mock fixtures declare fields only. Nothing selects them, so no naming verdict applies.
        String report = report(TestClassNamingConventionRule.RULE);

        assertFalse(report.contains("MockingRepositoryIT"), report);
        assertFalse(report.contains("MockingDaoIT"), report);
        assertFalse(report.contains("MockingGatewayIT"), report);
        assertFalse(report.contains("OrderRepository"), report);
        assertFalse(report.contains("PaymentGateway"), report);
    }

    @Test
    void flagsClassesWhoseOnlyTestsUseAComposedAnnotation() {
        // JUnit resolves @Test through a meta-annotation, so @FastTest methods really do run.
        // Matching direct annotations only would let this class look like coverage while Surefire
        // never selects it.
        String report = report(TestClassNamingConventionRule.RULE);

        assertTrue(report.contains("BadlyNamedComposedCase"), report);
    }

    @Test
    void pinsTheJunit5RootAnnotations() {
        // Only the three roots are configured: @ParameterizedTest and @RepeatedTest are themselves
        // meta-annotated with @TestTemplate and are reached through it, as is any consumer's own
        // composed annotation. Enumerating leaves instead reopens the "looks like coverage, never
        // runs" hole every time JUnit adds one.
        assertEquals(java.util.List.of(
                "org.junit.jupiter.api.Test",
                "org.junit.jupiter.api.TestFactory",
                "org.junit.jupiter.api.TestTemplate"), TestClassNamingConventionRule.JUNIT_TEST_ANNOTATIONS);
    }

    @Test
    void publicRuleIsFrozenAndIdPinned() {
        assertEquals("test.class-naming-convention", TestClassNamingConventionRule.rule.getDescription());
    }
}
