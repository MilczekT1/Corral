package io.github.milczekt1.corral.rules.spring.notransactionalonfinalorstatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.spring.notransactionalonfinalorstatic.fixtures.ArchiveService;
import io.github.milczekt1.corral.rules.spring.notransactionalonfinalorstatic.fixtures.CarrierBase;
import io.github.milczekt1.corral.rules.spring.notransactionalonfinalorstatic.fixtures.InventoryService;
import io.github.milczekt1.corral.rules.spring.notransactionalonfinalorstatic.fixtures.LedgerService;
import io.github.milczekt1.corral.rules.spring.notransactionalonfinalorstatic.fixtures.LegacyBillingService;
import io.github.milczekt1.corral.rules.spring.notransactionalonfinalorstatic.fixtures.Money;
import io.github.milczekt1.corral.rules.spring.notransactionalonfinalorstatic.fixtures.PaymentService;
import io.github.milczekt1.corral.rules.spring.notransactionalonfinalorstatic.fixtures.ReadOnlyTransaction;
import io.github.milczekt1.corral.rules.spring.notransactionalonfinalorstatic.fixtures.RefundService;
import io.github.milczekt1.corral.rules.spring.notransactionalonfinalorstatic.fixtures.ReportService;
import io.github.milczekt1.corral.rules.spring.notransactionalonfinalorstatic.fixtures.ShipmentService;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoTransactionalOnFinalOrStaticRuleTest {

    private static final String ID = "corral.spring.no-transactional-on-final-or-static";

    private static final JavaClasses EXAMPLES = new ClassFileImporter().importClasses(
            PaymentService.class, RefundService.class, LedgerService.class, InventoryService.class,
            LegacyBillingService.class, ReadOnlyTransaction.class, ReportService.class, CarrierBase.class,
            ShipmentService.class, ArchiveService.class, Money.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violations() {
        return NoTransactionalOnFinalOrStaticRule.DEFINITION.evaluate(EXAMPLES).getFailureReport().getDetails();
    }

    private static void assertFlagged(String method, String reason) {
        List<String> violations = violations();

        assertTrue(violations.stream().anyMatch(line -> line.contains(method + "(") && line.contains(reason)),
                String.join("\n", violations));
    }

    private static void assertNotReported(String subject) {
        String report = String.join("\n", violations());

        assertFalse(report.contains(subject), report);
    }

    @Test
    void flagsASpringTransactionalFinalMethod() {
        assertFlagged("PaymentService.settle", "is final");
    }

    @Test
    void flagsAJakartaTransactionalFinalMethod() {
        assertFlagged("InventoryService.reserveStock", "is final");
    }

    @Test
    void flagsAFinalMethodCarryingAComposedTransactionalAnnotation() {
        assertFlagged("ReportService.loadTotals", "is final");
    }

    @Test
    void flagsAFinalMethodInAClassAnnotatedAtClassLevel() {
        assertFlagged("RefundService.refund", "is final");
    }

    @Test
    void flagsAFinalMethodInAClassWhoseSuperclassIsAnnotated() {
        assertFlagged("ShipmentService.dispatchParcel", "is final");
    }

    @Test
    void flagsATransactionalMethodInAFinalClass() {
        assertFlagged("LedgerService.postJournal", "its class is final");
    }

    /** The reason is frozen text: removing a redundant {@code final} must not reword the line. */
    @Test
    void reportsAFinalMethodInAFinalClassAsFinal() {
        assertFlagged("LedgerService.closePeriod", "is final");
    }

    @Test
    void flagsASpringTransactionalStaticMethod() {
        assertFlagged("PaymentService.exchangeRate", "is static");
    }

    @Test
    void flagsAJavaxTransactionalStaticMethod() {
        assertFlagged("LegacyBillingService.chargeCard", "is static");
    }

    @Test
    void reportsABridgedMethodOnceNotOncePerBridge() {
        List<String> violations = violations();

        assertEquals(1, violations.stream().filter(line -> line.contains("ArchiveService.apply(")).count(),
                String.join("\n", violations));
    }

    @Test
    void reportsOneViolationPerUnproxyableTransactionalMethod() {
        assertEquals(10, violations().size(), String.join("\n", violations()));
    }

    @Test
    void ignoresAnOverridableTransactionalMethod() {
        assertNotReported("authorize");
    }

    @Test
    void ignoresAFinalMethodWithoutTheAnnotation() {
        assertNotReported("summarize");
    }

    @Test
    void ignoresAnUnannotatedMethodInAFinalClass() {
        assertNotReported("balance");
    }

    @Test
    void ignoresAFinalClassWithNoTransactionalMembers() {
        assertNotReported("Money");
    }

    @Test
    void ignoresAnOverridableMethodInAClassWhoseSuperclassIsAnnotated() {
        assertNotReported("trackParcel");
    }

    @Test
    void ignoresAnOverridableMethodInAClassAnnotatedAtClassLevel() {
        assertNotReported("listRefunds");
    }

    @Test
    void ignoresAStaticMethodMadeTransactionalOnlyByItsClass() {
        assertNotReported("vatRate");
    }

    @Test
    void ignoresPrivateMethodsWhateverMakesThemTransactional() {
        assertNotReported("recalculateFees");
        assertNotReported("roundAmount");
        assertNotReported("verifyTotals");
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoTransactionalOnFinalOrStaticRule.rule, ID, EXAMPLES);

        assertTrue(debt.contains("PaymentService.settle("), debt);
        assertTrue(debt.contains("PaymentService.exchangeRate("), debt);
        assertTrue(debt.contains("RefundService.refund("), debt);
        assertTrue(debt.contains("LedgerService.postJournal("), debt);
        assertTrue(debt.contains("LedgerService.closePeriod("), debt);
        assertTrue(debt.contains("InventoryService.reserveStock("), debt);
        assertTrue(debt.contains("LegacyBillingService.chargeCard("), debt);
        assertTrue(debt.contains("ReportService.loadTotals("), debt);
        assertTrue(debt.contains("ShipmentService.dispatchParcel("), debt);
        assertTrue(debt.contains("ArchiveService.apply("), debt);
    }
}
