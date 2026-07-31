package io.github.milczekt1.archrules.rules.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class TestClassNamingConventionTest {

    private static final JavaClasses FIXTURES = new ClassFileImporter()
            .importPackages("io.github.milczekt1.archrules.fixtures.testing");

    private static String report(ArchRule rule) {
        return String.join("\n", rule.allowEmptyShould(true).evaluate(FIXTURES).getFailureReport().getDetails());
    }

    @Test
    void flagsTestClassesThatSurefireWouldNeverRun() {
        String report = report(TestClassNamingConvention.RULE);

        assertTrue(report.contains("BadlyNamedTestCase"), report);
    }

    @Test
    void flagsClassesWhoseOnlyTestsAreParameterized() {
        // The false negative the rule's own `why` text describes: no @Test anywhere, so the old
        // @Test-only predicate never selected the class and Surefire never ran it either.
        String report = report(TestClassNamingConvention.RULE);

        assertTrue(report.contains("BadlyNamedParameterizedCase"), report);
    }

    @Test
    void acceptsConventionallyNamedTestClasses() {
        String report = report(TestClassNamingConvention.RULE);

        assertFalse(report.contains("WellNamedTest"), report);
        assertFalse(report.contains("PlainUnitTest"), report);
    }

    @Test
    void staysSilentOnJunit5NestedGroups() {
        // A @Nested class is imported as its own JavaClass named e.g. WhenEmpty and holds @Test
        // methods, but Surefire only ever selects the enclosing class — flagging it would be a
        // false positive whose only "fix" is a rename that changes nothing.
        String report = report(TestClassNamingConvention.RULE);

        assertFalse(report.contains("WhenEmpty"), report);
        assertFalse(report.contains("WhenPopulated"), report);
        assertFalse(report.contains("NestedGroupsTest"), report);
    }

    @Test
    void staysSilentOnClassesWithNoTestMethodsAtAll() {
        // The mock fixtures declare fields only. Nothing selects them, so no naming verdict applies.
        String report = report(TestClassNamingConvention.RULE);

        assertFalse(report.contains("MockingRepositoryIT"), report);
        assertFalse(report.contains("MockingDaoIntegrationTest"), report);
        assertFalse(report.contains("MockingGatewayIT"), report);
        assertFalse(report.contains("OrderRepository"), report);
        assertFalse(report.contains("PaymentGateway"), report);
    }

    @Test
    void detectsEveryJunit5TestAnnotation() {
        // Pinned as a configured constant: @TestFactory / @TestTemplate / @RepeatedTest have no
        // fixture, but omitting any of them reopens the "looks like coverage, never runs" hole.
        assertEquals(java.util.List.of(
                "org.junit.jupiter.api.Test",
                "org.junit.jupiter.api.RepeatedTest",
                "org.junit.jupiter.api.TestFactory",
                "org.junit.jupiter.api.TestTemplate",
                "org.junit.jupiter.params.ParameterizedTest"), TestClassNamingConvention.JUNIT_TEST_ANNOTATIONS);
    }

    @Test
    void publicRuleIsFrozenAndIdPinned() {
        assertEquals("test.class-naming-convention", TestClassNamingConvention.rule.getDescription());
    }
}
