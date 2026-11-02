package io.github.milczekt1.corral.rules.spring.norepositoryincontroller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures.TestOutputController;
import io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures.production.AbstractRefundEndpoint;
import io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures.production.ArchiveController;
import io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures.production.ArchiveDao;
import io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures.production.AuditController;
import io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures.production.CatalogController;
import io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures.production.CatalogRepository;
import io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures.production.InvoiceController;
import io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures.production.InvoiceRepository;
import io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures.production.LedgerController;
import io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures.production.LedgerDao;
import io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures.production.OrderController;
import io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures.production.OrderRepository;
import io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures.production.OrderSummary;
import io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures.production.PaymentController;
import io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures.production.PaymentService;
import io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures.production.PricingService;
import io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures.production.RefundController;
import io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures.production.ReportController;
import io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures.production.StockController;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import io.github.milczekt1.corral.rules.testsupport.FixtureVerdicts;
import io.github.milczekt1.corral.rules.testsupport.ProductionLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NoRepositoryInControllerRuleTest {

    private static final String ID = "corral.spring.no-repository-in-controller";

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        examples = ProductionLayout.importFixtures(NoRepositoryInControllerRuleTest.class, outputRoot.resolve("examples"));
    }

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violationsIn(JavaClasses classes) {
        return NoRepositoryInControllerRule.DEFINITION.evaluate(classes).getFailureReport().getDetails();
    }

    private static List<String> violations() {
        return violationsIn(examples);
    }

    /** A violation reads {@code <origin> <verb> <target>}; the origin ends at the first {@code "> "}. */
    private static String originOf(String violation) {
        return violation.substring(0, violation.indexOf("> ") + 1);
    }

    private static String targetOf(String violation) {
        return violation.substring(violation.indexOf("> ") + 1);
    }

    private static boolean names(String description, String type) {
        return description.contains("." + type + ">") || description.contains("." + type + ".");
    }

    private static void assertFlagged(String controller, String dependency) {
        List<String> violations = violations();

        assertTrue(violations.stream().anyMatch(line ->
                        names(originOf(line), controller) && names(targetOf(line), dependency)),
                String.join("\n", violations));
    }

    private static void assertNotReported(String controller) {
        List<String> violations = violations();

        assertFalse(violations.stream().anyMatch(line -> names(originOf(line), controller)),
                String.join("\n", violations));
    }

    private static void assertNoDependencyReportedOn(String type) {
        List<String> violations = violations();

        assertFalse(violations.stream().anyMatch(line -> names(targetOf(line), type)),
                String.join("\n", violations));
    }

    @Test
    void flagsARestControllerOnACrudRepositorySubtype() {
        assertFlagged("OrderController", "OrderRepository");
    }

    @Test
    void flagsARestControllerOnARepositoryAnnotatedClass() {
        assertFlagged("LedgerController", "LedgerDao");
    }

    @Test
    void flagsAControllerOnASubtypeOfTheSpringDataMarker() {
        assertFlagged("CatalogController", "CatalogRepository");
    }

    @Test
    void flagsARestControllerOnARepositoryDefinitionInterface() {
        assertFlagged("InvoiceController", "InvoiceRepository");
    }

    @Test
    void flagsARestControllerOnAClassMetaAnnotatedRepository() {
        assertFlagged("ArchiveController", "ArchiveDao");
    }

    @Test
    void flagsTheClassLiteralOfATypedGetBeanLookup() {
        List<String> violations = violations();

        assertTrue(violations.stream().anyMatch(line -> names(originOf(line), "StockController")
                        && line.contains(" references class object <") && names(targetOf(line), "OrderRepository")),
                String.join("\n", violations));
    }

    @Test
    void reportsEachDependencyOnARepository() {
        List<String> violations = violations();

        assertEquals(17, violations.size(), String.join("\n", violations));
    }

    @Test
    void ignoresTheServiceBesideAFlaggedRepository() {
        assertNoDependencyReportedOn("PricingService");
    }

    @Test
    void ignoresTheApplicationContextBehindATypedLookup() {
        assertNoDependencyReportedOn("ApplicationContext");
    }

    @Test
    void ignoresTheEntityAFlaggedRepositoryReturns() {
        assertNoDependencyReportedOn("OrderSummary");
    }

    @Test
    void ignoresAPageableHandlerParameter() {
        assertNoDependencyReportedOn("Pageable");
    }

    @Test
    void ignoresAServiceOnARepository() {
        assertNotReported("PaymentService");
    }

    @Test
    void ignoresASpringSecurityTypeThatOnlySharesTheSuffix() {
        assertNotReported("PaymentController");
    }

    @Test
    void ignoresAControllerWhoseBaseClassAloneUsesARepository() {
        assertNotReported("RefundController");
        assertNotReported("AbstractRefundEndpoint");
    }

    @Test
    void ignoresAProjectInterfaceNamedLikeARepository() {
        assertNotReported("AuditController");
    }

    @Test
    void ignoresAnUnannotatedDao() {
        assertNotReported("ReportController");
    }

    @Test
    void ignoresAControllerInTestOutput() {
        assertNotReported("TestOutputController");
    }

    /** The same class file as above, moved: only its location changed, so only scope can explain the difference. */
    @Test
    void flagsTheTestOutputControllerOnceItSitsInProductionOutput() throws IOException {
        Path productionOutput = ProductionLayout.copy(outputRoot.resolve("controller"),
                List.of(TestOutputController.class));

        List<String> violations = violationsIn(new ClassFileImporter().importPath(productionOutput));

        assertTrue(violations.stream().anyMatch(line -> names(originOf(line), "TestOutputController")),
                String.join("\n", violations));
    }

    @Test
    void everyFixtureMatchesItsVerdict() {
        FixtureVerdicts.assertMatch(NoRepositoryInControllerRuleTest.class, NoRepositoryInControllerRule.DEFINITION, examples);
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoRepositoryInControllerRule.rule, ID, examples);

        assertTrue(debt.contains(".OrderController.orders> has type <"), debt);
        assertTrue(debt.contains(".LedgerController.ledger> has type <"), debt);
        assertTrue(debt.contains(".CatalogController.catalog> has type <"), debt);
        assertTrue(debt.contains(".StockController.count()> references class object <"), debt);
        assertTrue(debt.contains(".InvoiceController.invoices> has type <"), debt);
        assertTrue(debt.contains(".ArchiveController.archive> has type <"), debt);
    }
}
