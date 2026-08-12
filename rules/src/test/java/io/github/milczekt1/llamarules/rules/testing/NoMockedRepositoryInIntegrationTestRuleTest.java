package io.github.milczekt1.llamarules.rules.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class NoMockedRepositoryInIntegrationTestRuleTest {

    private static final JavaClasses FIXTURES = new ClassFileImporter()
            .importPackages("io.github.milczekt1.llamarules.fixtures.testing");

    private static String report(ArchRule rule) {
        return String.join("\n", rule.allowEmptyShould(true).evaluate(FIXTURES).getFailureReport().getDetails());
    }

    @Test
    void flagsMockedRepositoryInAClassNamedIT() {
        String report = report(NoMockedRepositoryInIntegrationTestRule.RULE);

        assertTrue(report.contains("MockingRepositoryIT"), report);
        assertTrue(report.contains("orderRepository"), report);
    }

    @Test
    void flagsMockedDaoInAClassNamedIT() {
        String report = report(NoMockedRepositoryInIntegrationTestRule.RULE);

        assertTrue(report.contains("MockingDaoIT"), report);
        assertTrue(report.contains("orderDao"), report);
    }

    @Test
    void flagsMockedRepositoryInheritedFromABaseTestClass() {
        String report = report(NoMockedRepositoryInIntegrationTestRule.RULE);

        assertTrue(report.contains("inheritedOrderRepository"),
                "a mock parked on a base class still belongs to the IT that inherits it: " + report);
        assertTrue(report.contains("inherited by InheritingMockingRepositoryIT"),
                "the report must name the IT to fix, not just the base class holding the field: " + report);
    }

    @Test
    void staysSilentAboutTheBaseClassItself() {
        String report = report(NoMockedRepositoryInIntegrationTestRule.RULE);

        assertFalse(report.contains("inherited by AbstractMockingRepositoryTestBase"),
                "the base class is not named IT, so it is not an integration test: " + report);
    }

    @Test
    void allowsMockingNonPersistenceCollaboratorsInIntegrationTests() {
        String report = report(NoMockedRepositoryInIntegrationTestRule.RULE);

        assertFalse(report.contains("MockingGatewayIT"),
                "only Repository/Dao types are forbidden: " + report);
    }

    @Test
    void allowsUnitTestsToMockRepositories() {
        String report = report(NoMockedRepositoryInIntegrationTestRule.RULE);

        assertFalse(report.contains("PlainUnitTest"),
                "the rule targets integration tests only: " + report);
    }

    @Test
    void coversEveryForbiddenMockAnnotation() {
        // MockBean does not exist in Spring Boot 4, so it has no fixture; it is listed for
        // consumers still on Boot 3 and is verified here as a configured constant.
        assertTrue(NoMockedRepositoryInIntegrationTestRule.FORBIDDEN_MOCK_ANNOTATIONS.containsAll(java.util.List.of(
                "org.mockito.Mock",
                "org.springframework.test.context.bean.override.mockito.MockitoBean",
                "org.springframework.boot.test.mock.mockito.MockBean")));
    }

    @Test
    void publicRuleIsFrozenAndIdPinned() {
        assertEquals("test.no-mocked-repository-in-integration-test",
                NoMockedRepositoryInIntegrationTestRule.rule.getDescription());
    }
}
