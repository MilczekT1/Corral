package io.github.milczekt1.corral.rules.spring.notransactionalonprivatemethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.rules.spring.notransactionalonprivatemethod.fixtures.InventoryService;
import io.github.milczekt1.corral.rules.spring.notransactionalonprivatemethod.fixtures.LedgerService;
import io.github.milczekt1.corral.rules.spring.notransactionalonprivatemethod.fixtures.LegacyBillingService;
import io.github.milczekt1.corral.rules.spring.notransactionalonprivatemethod.fixtures.PaymentService;
import io.github.milczekt1.corral.rules.spring.notransactionalonprivatemethod.fixtures.ReadOnlyTransaction;
import io.github.milczekt1.corral.rules.spring.notransactionalonprivatemethod.fixtures.RefundService;
import io.github.milczekt1.corral.rules.spring.notransactionalonprivatemethod.fixtures.ReportService;
import io.github.milczekt1.corral.rules.spring.notransactionalonprivatemethod.fixtures.ShipmentService;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoTransactionalOnPrivateMethodRuleTest {

    private static final String ID = "corral.spring.no-transactional-on-private-method";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoTransactionalOnPrivateMethodRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(EXAMPLES);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("LedgerService.postEntries("), debt);
            assertTrue(debt.contains("InventoryService.reserveStock("), debt);
            assertTrue(debt.contains("LegacyBillingService.chargeCard("), debt);
            assertTrue(debt.contains("ReportService.loadTotals("), debt);
            assertTrue(debt.contains("ShipmentService.dispatchParcel("), debt);
            assertTrue(debt.contains("RefundService.issueRefund("), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
