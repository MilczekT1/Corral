package io.github.milczekt1.corral.rules.spring.notransactionalonfinalorstatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
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
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoTransactionalOnFinalOrStaticRuleTest {

    private static final String ID = "corral.spring.no-transactional-on-final-or-static";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

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
        assertEquals(9, violations().size(), String.join("\n", violations()));
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

    /**
     * {@link FreezingArchRule#persistIn}, not {@code freeze.store}: a frozen rule captures its store
     * when constructed, which is class initialisation, so naming one here races class loading. Only
     * the path goes on the process-wide {@link ArchConfiguration}.
     *
     * <p>Reseed with {@code -Darchunit.freeze.store.default.allowStoreCreation=true}, then commit.
     */
    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        ArchConfiguration.get().setProperty("freeze.store.default.path", STORE_PATH);
        try {
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoTransactionalOnFinalOrStaticRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(EXAMPLES);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("PaymentService.settle("), debt);
            assertTrue(debt.contains("PaymentService.exchangeRate("), debt);
            assertTrue(debt.contains("RefundService.refund("), debt);
            assertTrue(debt.contains("LedgerService.postJournal("), debt);
            assertTrue(debt.contains("InventoryService.reserveStock("), debt);
            assertTrue(debt.contains("LegacyBillingService.chargeCard("), debt);
            assertTrue(debt.contains("ReportService.loadTotals("), debt);
            assertTrue(debt.contains("ShipmentService.dispatchParcel("), debt);
            assertTrue(debt.contains("ArchiveService.apply("), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
