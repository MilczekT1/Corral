package io.github.milczekt1.corral.rules.spring.notransactionalincontroller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures.AbstractClaimEndpoint;
import io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures.AbstractJournalEndpoint;
import io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures.ArchiveController;
import io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures.ArchiveOperations;
import io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures.AuditController;
import io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures.BillingController;
import io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures.CatalogController;
import io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures.CustomerController;
import io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures.DigestController;
import io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures.DigestOperations;
import io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures.EntityWriter;
import io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures.ExportController;
import io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures.ExportOperations;
import io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures.InvoiceController;
import io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures.JournalController;
import io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures.LedgerController;
import io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures.NotificationController;
import io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures.OrderController;
import io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures.PaymentController;
import io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures.PaymentService;
import io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures.PriceCalculator;
import io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures.ReadOnlyTransaction;
import io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures.RefundController;
import io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures.ReturnController;
import io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures.StockController;
import io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures.TestOutputController;
import io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures.TransactionalAuditBase;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import io.github.milczekt1.corral.rules.testsupport.ProductionLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The rule is scoped to production classes, and every fixture compiles into test output. So the
 * examples are copied into a Gradle main-output layout and imported from there;
 * {@link TestOutputController} alone is imported from where it compiled.
 */
class NoTransactionalInControllerRuleTest {

    private static final String ID = "corral.spring.no-transactional-in-controller";

    private static final List<Class<?>> PRODUCTION_EXAMPLES = List.of(
            OrderController.class, LedgerController.class, InvoiceController.class, BillingController.class,
            CatalogController.class, ReadOnlyTransaction.class, StockController.class, RefundController.class,
            ReturnController.class, AbstractClaimEndpoint.class, AuditController.class,
            TransactionalAuditBase.class, ArchiveController.class, ArchiveOperations.class,
            ExportController.class, ExportOperations.class, CustomerController.class, EntityWriter.class,
            PaymentController.class, PaymentService.class, PriceCalculator.class, NotificationController.class,
            JournalController.class, AbstractJournalEndpoint.class, DigestController.class, DigestOperations.class);

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        Path productionOutput = ProductionLayout.copy(outputRoot.resolve("examples"), PRODUCTION_EXAMPLES);
        examples = new ClassFileImporter().importLocations(
                ProductionLayout.withTestOutput(productionOutput, TestOutputController.class));

        assertEquals(PRODUCTION_EXAMPLES.size() + 1, examples.size(),
                "every example must be imported, or the ignore assertions below are vacuous");
    }

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violationsIn(JavaClasses classes) {
        return NoTransactionalInControllerRule.DEFINITION.evaluate(classes).getFailureReport().getDetails();
    }

    private static List<String> violations() {
        return violationsIn(examples);
    }

    private static String controllerOf(String violation) {
        return violation.substring(0, violation.indexOf("> ") + 1);
    }

    private static boolean names(String description, String type) {
        return description.contains("." + type + ">") || description.contains("." + type + ".");
    }

    private static List<String> violationsOn(String controller) {
        return violations().stream().filter(line -> names(controllerOf(line), controller)).toList();
    }

    private static void assertReportedOnce(String controller, String finding) {
        List<String> lines = violationsOn(controller);

        assertEquals(1, lines.size(), String.join("\n", violations()));
        assertTrue(lines.get(0).contains(finding), lines.get(0));
    }

    private static void assertNotReported(String controller) {
        assertEquals(List.of(), violationsOn(controller));
    }

    @Test
    void flagsATransactionalHandlerAndNotItsPlainSibling() {
        assertReportedOnce("OrderController", " has @Transactional method <");
        assertTrue(violationsOn("OrderController").get(0).contains(".OrderController.place("));
    }

    @Test
    void flagsAControllerAnnotatedAtClassLevel() {
        assertReportedOnce("LedgerController", "LedgerController> is @Transactional");
    }

    @Test
    void flagsJakartaTransactional() {
        assertReportedOnce("InvoiceController", ".InvoiceController.issue()>");
    }

    @Test
    void flagsJavaxTransactional() {
        assertReportedOnce("BillingController", ".BillingController.bill()>");
    }

    @Test
    void flagsAComposedAnnotationMetaAnnotatedTransactional() {
        assertReportedOnce("CatalogController", ".CatalogController.browse()>");
    }

    @Test
    void flagsAPrivateHelper() {
        assertReportedOnce("StockController", ".StockController.reserve()>");
    }

    @Test
    void flagsAMethodInheritedFromAnUnannotatedBaseClass() {
        assertReportedOnce("RefundController", ".AbstractClaimEndpoint.settle()>");
    }

    @Test
    void reportsASharedBaseClassMethodOncePerController() {
        assertReportedOnce("RefundController", ".AbstractClaimEndpoint.settle()>");
        assertReportedOnce("ReturnController", ".AbstractClaimEndpoint.settle()>");
    }

    @Test
    void flagsAClassLevelAnnotationOnABaseClass() {
        assertReportedOnce("AuditController", " inherits @Transactional from <");
        assertTrue(violationsOn("AuditController").get(0).endsWith(".TransactionalAuditBase>"));
    }

    @Test
    void flagsAClassLevelAnnotationTwoSuperclassesUp() {
        assertReportedOnce("JournalController", " inherits @Transactional from <");
        assertTrue(violationsOn("JournalController").get(0).endsWith(".TransactionalAuditBase>"));
    }

    @Test
    void flagsATypeLevelAnnotationOnASuperinterface() {
        assertReportedOnce("DigestController", " inherits @Transactional from <");
        assertTrue(violationsOn("DigestController").get(0).endsWith(".ExportOperations>"));
    }

    @Test
    void flagsAnInterfaceMethodTheControllerImplements() {
        assertReportedOnce("ArchiveController", ".ArchiveOperations.purge()>");
    }

    @Test
    void flagsATypeLevelAnnotationOnAnInterface() {
        assertReportedOnce("ExportController", " inherits @Transactional from <");
        assertTrue(violationsOn("ExportController").get(0).endsWith(".ExportOperations>"));
    }

    @Test
    void reportsAnOverrideOnceDespiteItsBridgeMethod() {
        assertReportedOnce("CustomerController", ".CustomerController.save(java.lang.String)>");
    }

    @Test
    void namesTheAnnotationNeverItsPackage() {
        List<String> violations = violations();

        assertFalse(violations.stream().anyMatch(line -> line.contains("transaction.Transactional")),
                String.join("\n", violations));
    }

    @Test
    void reportsEachTransactionalElement() {
        List<String> violations = violations();

        assertEquals(14, violations.size(), String.join("\n", violations));
    }

    @Test
    void ignoresAControllerDelegatingToATransactionalService() {
        assertNotReported("PaymentController");
        assertNotReported("PaymentService");
    }

    @Test
    void ignoresTransactionalOnAClassWithNoStereotype() {
        assertNotReported("PriceCalculator");
    }

    @Test
    void ignoresATransactionalEventListener() {
        assertNotReported("NotificationController");
    }

    @Test
    void ignoresTheBaseTypesThemselves() {
        assertNotReported("AbstractClaimEndpoint");
        assertNotReported("TransactionalAuditBase");
        assertNotReported("ArchiveOperations");
        assertNotReported("ExportOperations");
        assertNotReported("AbstractJournalEndpoint");
        assertNotReported("DigestOperations");
    }

    @Test
    void ignoresAControllerInTestOutput() {
        assertNotReported("TestOutputController");
    }

    @Test
    void flagsTheTestOutputControllerOnceItSitsInProductionOutput() throws IOException {
        Path productionOutput = ProductionLayout.copy(outputRoot.resolve("controller"),
                List.of(TestOutputController.class));

        List<String> violations = violationsIn(new ClassFileImporter().importPath(productionOutput));

        assertTrue(violations.stream().anyMatch(line -> names(controllerOf(line), "TestOutputController")),
                String.join("\n", violations));
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoTransactionalInControllerRule.rule, ID, examples);

        assertTrue(debt.contains(".OrderController> has @Transactional method <"), debt);
        assertTrue(debt.contains(".LedgerController> is @Transactional"), debt);
        assertTrue(debt.contains(".AuditController> inherits @Transactional from <"), debt);
        assertTrue(debt.contains(".RefundController> has @Transactional method <"), debt);
        assertTrue(debt.contains(".ReturnController> has @Transactional method <"), debt);
    }
}
