package io.github.milczekt1.corral.rules.spring.notransactionalonprivatemethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.spring.notransactionalonprivatemethod.fixtures.InventoryService;
import io.github.milczekt1.corral.rules.spring.notransactionalonprivatemethod.fixtures.LedgerService;
import io.github.milczekt1.corral.rules.spring.notransactionalonprivatemethod.fixtures.LegacyBillingService;
import io.github.milczekt1.corral.rules.spring.notransactionalonprivatemethod.fixtures.PaymentService;
import io.github.milczekt1.corral.rules.spring.notransactionalonprivatemethod.fixtures.ReadOnlyTransaction;
import io.github.milczekt1.corral.rules.spring.notransactionalonprivatemethod.fixtures.RefundService;
import io.github.milczekt1.corral.rules.spring.notransactionalonprivatemethod.fixtures.ReportService;
import io.github.milczekt1.corral.rules.spring.notransactionalonprivatemethod.fixtures.ShipmentService;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoTransactionalOnPrivateMethodRuleTest {

    private static final String ID = "corral.spring.no-transactional-on-private-method";

    private static final JavaClasses EXAMPLES = new ClassFileImporter().importClasses(
            LedgerService.class, InventoryService.class, LegacyBillingService.class,
            ReadOnlyTransaction.class, ReportService.class, ShipmentService.class,
            RefundService.class, PaymentService.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violations() {
        return NoTransactionalOnPrivateMethodRule.DEFINITION.evaluate(EXAMPLES).getFailureReport().getDetails();
    }

    private static void assertFlagged(String method) {
        List<String> violations = violations();

        assertTrue(violations.stream().anyMatch(line -> line.contains(method + "(")), String.join("\n", violations));
    }

    private static void assertNotReported(String subject) {
        String report = String.join("\n", violations());

        assertFalse(report.contains(subject), report);
    }

    @Test
    void flagsASpringTransactionalPrivateMethod() {
        assertFlagged("LedgerService.postEntries");
    }

    @Test
    void flagsAJakartaTransactionalPrivateMethod() {
        assertFlagged("InventoryService.reserveStock");
    }

    @Test
    void flagsAJavaxTransactionalPrivateMethod() {
        assertFlagged("LegacyBillingService.chargeCard");
    }

    @Test
    void flagsAPrivateMethodCarryingAComposedTransactionalAnnotation() {
        assertFlagged("ReportService.loadTotals");
    }

    @Test
    void flagsAPrivateFinalMethod() {
        assertFlagged("ShipmentService.dispatchParcel");
    }

    @Test
    void flagsAPrivateMethodOnAFinalClass() {
        assertFlagged("RefundService.issueRefund");
    }

    @Test
    void reportsOneViolationPerPrivateTransactionalMethod() {
        assertEquals(6, violations().size(), String.join("\n", violations()));
    }

    @Test
    void ignoresAPublicTransactionalMethod() {
        assertNotReported("openBatch");
    }

    @Test
    void ignoresAProtectedTransactionalMethod() {
        assertNotReported("closeBatch");
    }

    @Test
    void ignoresAPackagePrivateTransactionalMethod() {
        assertNotReported("rebalance");
    }

    @Test
    void ignoresAPrivateMethodWithoutTheAnnotation() {
        assertNotReported("audit");
    }

    @Test
    void ignoresAPrivateMethodInAClassAnnotatedAtClassLevel() {
        assertNotReported("PaymentService");
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoTransactionalOnPrivateMethodRule.rule, ID, EXAMPLES);

        assertTrue(debt.contains("LedgerService.postEntries("), debt);
        assertTrue(debt.contains("InventoryService.reserveStock("), debt);
        assertTrue(debt.contains("LegacyBillingService.chargeCard("), debt);
        assertTrue(debt.contains("ReportService.loadTotals("), debt);
        assertTrue(debt.contains("ShipmentService.dispatchParcel("), debt);
        assertTrue(debt.contains("RefundService.issueRefund("), debt);
    }
}
