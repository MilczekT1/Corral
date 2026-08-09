package io.github.milczekt1.llamarules.rules.rules.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class NoMockedRepositoryInIntegrationTestTest {

    private static final JavaClasses FIXTURES = new ClassFileImporter()
            .importPackages("io.github.milczekt1.llamarules.rules.fixtures.testing");

    private static String report(ArchRule rule) {
        return String.join("\n", rule.allowEmptyShould(true).evaluate(FIXTURES).getFailureReport().getDetails());
    }

    @Test
    void flagsMockedRepositoryInAClassNamedIT() {
        String report = report(NoMockedRepositoryInIntegrationTest.RULE);

        assertTrue(report.contains("MockingRepositoryIT"), report);
        assertTrue(report.contains("orderRepository"), report);
    }

    @Test
    void flagsMockedDaoInAClassNamedIntegrationTest() {
        String report = report(NoMockedRepositoryInIntegrationTest.RULE);

        assertTrue(report.contains("MockingDaoIntegrationTest"), report);
        assertTrue(report.contains("orderDao"), report);
    }

    @Test
    void allowsMockingNonPersistenceCollaboratorsInIntegrationTests() {
        String report = report(NoMockedRepositoryInIntegrationTest.RULE);

        assertFalse(report.contains("MockingGatewayIT"),
                "only Repository/Dao types are forbidden: " + report);
    }

    @Test
    void allowsUnitTestsToMockRepositories() {
        String report = report(NoMockedRepositoryInIntegrationTest.RULE);

        assertFalse(report.contains("PlainUnitTest"),
                "the rule targets integration tests only: " + report);
    }

    @Test
    void coversEveryForbiddenMockAnnotationIncludingTheRemovedSpringBootOne() {
        // MockBean was removed in Spring Boot 4, so it has no fixture; it stays in the list for
        // consumers still on Boot 3 and is verified here as a configured constant.
        assertTrue(NoMockedRepositoryInIntegrationTest.FORBIDDEN_MOCK_ANNOTATIONS.containsAll(java.util.List.of(
                "org.mockito.Mock",
                "org.springframework.test.context.bean.override.mockito.MockitoBean",
                "org.springframework.boot.test.mock.mockito.MockBean")));
    }

    @Test
    void publicRuleIsFrozenAndIdPinned() {
        assertEquals("test.no-mocked-repository-in-integration-test",
                NoMockedRepositoryInIntegrationTest.rule.getDescription());
    }
}
