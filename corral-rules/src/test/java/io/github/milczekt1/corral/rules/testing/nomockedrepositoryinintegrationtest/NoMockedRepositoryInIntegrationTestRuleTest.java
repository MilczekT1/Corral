package io.github.milczekt1.corral.rules.testing.nomockedrepositoryinintegrationtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * The examples are nested and static, so no runner selects them — a top-level {@code *IT} outside a
 * {@code fixtures} package is run by Failsafe for real. ArchUnit reads a nested class's unqualified
 * simple name, which is what the rule's {@code haveSimpleNameEndingWith("IT")} clause matches.
 */
class NoMockedRepositoryInIntegrationTestRuleTest {

    interface OrderRepository {
        String findById(String id);
    }

    interface OrderDao {
        String load(String id);
    }

    /** Not a repository or dao — mocking this in an integration test is fine. */
    interface PaymentGateway {
        boolean charge(long amountMinor);
    }

    /** MUST FLAG: {@code @Mock} on a type ending in Repository, in a class named {@code *IT}. */
    static class RepositoryMockingIT {

        @Mock
        OrderRepository mockedOrderRepository;
    }

    /** MUST FLAG: the Dao half of the type clause, mocked through Spring's {@code @MockitoBean}. */
    static class DaoMockingIT {

        @MockitoBean
        OrderDao mockedOrderDao;
    }

    /** MUST IGNORE: mocked, but a gateway is not a persistence type. */
    static class GatewayMockingIT {

        @Mock
        PaymentGateway mockedPaymentGateway;
    }

    /** MUST IGNORE: a real repository in an integration test is exactly the point of one. */
    static class RealRepositoryIT {

        OrderRepository unmockedOrderRepository;
    }

    /**
     * MUST IGNORE in its own right: holds the mock, but is not named {@code *IT}, so it is not an
     * integration test. Only {@link InheritedMockingIT} is.
     */
    abstract static class SharedMockBase {

        @Mock
        OrderRepository inheritedOrderRepository;
    }

    /** MUST FLAG: declares no field itself — the mock arrives through {@link SharedMockBase}. */
    static class InheritedMockingIT extends SharedMockBase {
    }

    /** MUST IGNORE: mocking a repository is what a unit test is for. */
    static class RepositoryMockingUnitTest {

        @Mock
        OrderRepository unitTestOrderRepository;
    }

    private static final String ID = "corral.test.no-mocked-repository-in-integration-test";

    private static final JavaClasses EXAMPLES = new ClassFileImporter().importClasses(
            OrderRepository.class, OrderDao.class, PaymentGateway.class,
            RepositoryMockingIT.class, DaoMockingIT.class, GatewayMockingIT.class,
            RealRepositoryIT.class, SharedMockBase.class, InheritedMockingIT.class,
            RepositoryMockingUnitTest.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static String report() {
        return String.join("\n", NoMockedRepositoryInIntegrationTestRule.DEFINITION
                .allowEmptyShould(true).evaluate(EXAMPLES).getFailureReport().getDetails());
    }

    @Test
    void flagsAMockedRepositoryInAnIntegrationTest() {
        String report = report();

        assertTrue(report.contains("RepositoryMockingIT"), report);
        assertTrue(report.contains("mockedOrderRepository"), report);
    }

    @Test
    void flagsAMockedDaoInAnIntegrationTest() {
        String report = report();

        assertTrue(report.contains("DaoMockingIT"), report);
        assertTrue(report.contains("mockedOrderDao"), report);
    }

    @Test
    void flagsAMockInheritedFromABaseTestClass() {
        String report = report();

        assertTrue(report.contains("inheritedOrderRepository"),
                "a mock parked on a base class still belongs to the IT that inherits it: " + report);
        assertTrue(report.contains("inherited by InheritedMockingIT"),
                "the report must name the IT to fix, not just the base class holding the field: " + report);
    }

    @Test
    void staysSilentAboutTheBaseClassItself() {
        String report = report();

        assertFalse(report.contains("inherited by SharedMockBase"),
                "the base class is not named IT, so it is not an integration test: " + report);
    }

    @Test
    void allowsMockingNonPersistenceCollaboratorsInIntegrationTests() {
        String report = report();

        assertFalse(report.contains("GatewayMockingIT"),
                "only Repository and Dao types are forbidden: " + report);
    }

    @Test
    void allowsAnUnmockedRepositoryInAnIntegrationTest() {
        String report = report();

        assertFalse(report.contains("unmockedOrderRepository"),
                "the field is a repository but nothing mocks it, which is the point of an IT: " + report);
    }

    @Test
    void allowsUnitTestsToMockRepositories() {
        String report = report();

        assertFalse(report.contains("RepositoryMockingUnitTest"),
                "the rule targets integration tests only: " + report);
    }

    @Test
    void coversEveryForbiddenMockAnnotation() {
        // MockBean is gone in Spring Boot 4, so it has no example — only this constant check.
        assertEquals(List.of(
                        "org.mockito.Mock",
                        "org.springframework.test.context.bean.override.mockito.MockitoBean",
                        "org.springframework.boot.test.mock.mockito.MockBean"),
                NoMockedRepositoryInIntegrationTestRule.FORBIDDEN_MOCK_ANNOTATIONS);
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoMockedRepositoryInIntegrationTestRule.rule, ID, EXAMPLES);

        assertTrue(debt.contains("mockedOrderRepository"), debt);
        assertFalse(debt.contains("mockedPaymentGateway"),
                "an allowed mock must never reach the store: " + debt);
    }
}
