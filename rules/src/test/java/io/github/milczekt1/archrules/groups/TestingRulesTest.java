package io.github.milczekt1.archrules.groups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class TestingRulesTest {

    private static final JavaClasses FIXTURES = new ClassFileImporter()
            .importPackages("io.github.milczekt1.archrules.fixtures.testing");

    private static String report(ArchRule rule) {
        return String.join("\n", rule.allowEmptyShould(true).evaluate(FIXTURES).getFailureReport().getDetails());
    }

    @Test
    void flagsMockedRepositoryInAClassNamedIT() {
        String report = report(TestingRules.NO_MOCKED_REPOS_IN_IT_RULE);

        assertTrue(report.contains("MockingRepositoryIT"), report);
        assertTrue(report.contains("orderRepository"), report);
    }

    @Test
    void flagsMockedDaoInAClassNamedIntegrationTest() {
        String report = report(TestingRules.NO_MOCKED_REPOS_IN_IT_RULE);

        assertTrue(report.contains("MockingDaoIntegrationTest"), report);
        assertTrue(report.contains("orderDao"), report);
    }

    @Test
    void allowsMockingNonPersistenceCollaboratorsInIntegrationTests() {
        String report = report(TestingRules.NO_MOCKED_REPOS_IN_IT_RULE);

        assertFalse(report.contains("MockingGatewayIT"),
                "only Repository/Dao types are forbidden: " + report);
    }

    @Test
    void allowsUnitTestsToMockRepositories() {
        String report = report(TestingRules.NO_MOCKED_REPOS_IN_IT_RULE);

        assertFalse(report.contains("PlainUnitTest"),
                "the rule targets integration tests only: " + report);
    }

    @Test
    void flagsTestClassesThatSurefireWouldNeverRun() {
        String report = report(TestingRules.TEST_NAMING_RULE);

        assertTrue(report.contains("BadlyNamedTestCase"), report);
    }

    @Test
    void flagsClassesWhoseOnlyTestsAreParameterized() {
        // The false negative the rule's own `why` text describes: no @Test anywhere, so the old
        // @Test-only predicate never selected the class and Surefire never ran it either.
        String report = report(TestingRules.TEST_NAMING_RULE);

        assertTrue(report.contains("BadlyNamedParameterizedCase"), report);
    }

    @Test
    void acceptsConventionallyNamedTestClasses() {
        String report = report(TestingRules.TEST_NAMING_RULE);

        assertFalse(report.contains("WellNamedTest"), report);
        assertFalse(report.contains("PlainUnitTest"), report);
    }

    @Test
    void staysSilentOnJunit5NestedGroups() {
        // A @Nested class is imported as its own JavaClass named e.g. WhenEmpty and holds @Test
        // methods, but Surefire only ever selects the enclosing class — flagging it would be a
        // false positive whose only "fix" is a rename that changes nothing.
        String report = report(TestingRules.TEST_NAMING_RULE);

        assertFalse(report.contains("WhenEmpty"), report);
        assertFalse(report.contains("WhenPopulated"), report);
        assertFalse(report.contains("NestedGroupsTest"), report);
    }

    @Test
    void staysSilentOnClassesWithNoTestMethodsAtAll() {
        // The mock fixtures declare fields only. Nothing selects them, so no naming verdict applies.
        String report = report(TestingRules.TEST_NAMING_RULE);

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
                "org.junit.jupiter.params.ParameterizedTest"), TestingRules.JUNIT_TEST_ANNOTATIONS);
    }

    @Test
    void coversEveryForbiddenMockAnnotationIncludingTheRemovedSpringBootOne() {
        // MockBean was removed in Spring Boot 4, so it has no fixture; it stays in the list for
        // consumers still on Boot 3 and is verified here as a configured constant.
        assertTrue(TestingRules.FORBIDDEN_MOCK_ANNOTATIONS.containsAll(java.util.List.of(
                "org.mockito.Mock",
                "org.springframework.test.context.bean.override.mockito.MockitoBean",
                "org.springframework.boot.test.mock.mockito.MockBean")));
    }

    @Test
    void everyPublicRuleIsFrozenAndIdPinned() {
        assertEquals("test.no-mocked-repository-in-integration-test",
                TestingRules.integrationTestsMustNotMockRepositoriesOrDaos.getDescription());
        assertEquals("test.class-naming-convention",
                TestingRules.testClassNamingConvention.getDescription());
    }
}
