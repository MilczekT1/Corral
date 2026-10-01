package io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod.fixtures.AsyncWorkerBase;
import io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod.fixtures.AuditService;
import io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod.fixtures.BackgroundTask;
import io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod.fixtures.CourierDispatcher;
import io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod.fixtures.ExportService;
import io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod.fixtures.MailerService;
import io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod.fixtures.OrderService;
import io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod.fixtures.PaymentService;
import io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod.fixtures.ReportService;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoAsyncOnUnproxyableMethodRuleTest {

    private static final String ID = "corral.spring.no-async-on-unproxyable-method";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    private static final JavaClasses EXAMPLES = new ClassFileImporter().importClasses(
            OrderService.class, ReportService.class, PaymentService.class, AsyncWorkerBase.class,
            CourierDispatcher.class, MailerService.class, BackgroundTask.class, AuditService.class,
            ExportService.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violations() {
        return NoAsyncOnUnproxyableMethodRule.DEFINITION.evaluate(EXAMPLES).getFailureReport().getDetails();
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
    void flagsAPrivateAsyncMethod() {
        assertFlagged("OrderService.notifyWarehouse", "is private");
    }

    @Test
    void flagsAFinalAsyncMethod() {
        assertFlagged("OrderService.notifyCourier", "is final");
    }

    @Test
    void flagsAStaticAsyncMethod() {
        assertFlagged("OrderService.notifyAudit", "is static");
    }

    @Test
    void reportsAStaticFinalMethodAsStatic() {
        assertFlagged("OrderService.notifyTax", "is static");
    }

    @Test
    void reportsAPrivateFinalMethodAsPrivate() {
        assertFlagged("OrderService.notifyBilling", "is private");
    }

    @Test
    void reportsAPrivateStaticMethodAsPrivate() {
        assertFlagged("OrderService.purgeOutbox", "is private");
    }

    @Test
    void flagsAFinalMethodInAClassAnnotatedAtClassLevel() {
        assertFlagged("ReportService.render", "is final");
    }

    @Test
    void flagsAFinalMethodInAClassWhoseSuperclassIsAnnotated() {
        assertFlagged("CourierDispatcher.dispatchCourier", "is final");
    }

    @Test
    void flagsAnAsyncMethodInAFinalClass() {
        assertFlagged("MailerService.sendDigest", "its class is final");
    }

    @Test
    void reportsAPrivateMethodInAFinalClassAsPrivate() {
        assertFlagged("MailerService.compressDigest", "is private");
    }

    @Test
    void flagsAPrivateMethodCarryingAComposedAsyncAnnotation() {
        assertFlagged("AuditService.recordAudit", "is private");
    }

    @Test
    void reportsABridgedMethodOnceNotOncePerBridge() {
        List<String> violations = violations();

        assertEquals(1, violations.stream().filter(line -> line.contains("ExportService.apply(")).count(),
                String.join("\n", violations));
    }

    @Test
    void reportsOneViolationPerUnproxyableAsyncMethod() {
        assertEquals(12, violations().size(), String.join("\n", violations()));
    }

    @Test
    void ignoresAnOverridableAsyncMethod() {
        assertNotReported("publishOrder");
        assertNotReported("settlePayment");
    }

    @Test
    void ignoresProtectedAndPackagePrivateAsyncMethods() {
        assertNotReported("refundPayment");
        assertNotReported("notifyLedger");
    }

    @Test
    void ignoresMethodsWithoutTheAnnotation() {
        assertNotReported("listOrders");
        assertNotReported("roundCents");
        assertNotReported("describe");
        assertNotReported("preview");
    }

    @Test
    void ignoresAnOverridableMethodInAClassAnnotatedAtClassLevel() {
        assertNotReported("exportCsv");
    }

    @Test
    void ignoresAnOverridableMethodInAClassWhoseSuperclassIsAnnotated() {
        assertNotReported("queueDepth");
    }

    @Test
    void ignoresPrivateHelpersMadeAsyncOnlyByTheirClass() {
        assertNotReported("formatRows");
        assertNotReported("trimRows");
        assertNotReported("stampHeader");
    }

    @Test
    void ignoresAStaticHelperMadeAsyncOnlyByItsClass() {
        assertNotReported("pageSize");
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoAsyncOnUnproxyableMethodRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(EXAMPLES);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("OrderService.notifyWarehouse("), debt);
            assertTrue(debt.contains("OrderService.notifyCourier("), debt);
            assertTrue(debt.contains("OrderService.notifyAudit("), debt);
            assertTrue(debt.contains("OrderService.notifyTax("), debt);
            assertTrue(debt.contains("OrderService.notifyBilling("), debt);
            assertTrue(debt.contains("OrderService.purgeOutbox("), debt);
            assertTrue(debt.contains("ReportService.render("), debt);
            assertTrue(debt.contains("CourierDispatcher.dispatchCourier("), debt);
            assertTrue(debt.contains("MailerService.sendDigest("), debt);
            assertTrue(debt.contains("MailerService.compressDigest("), debt);
            assertTrue(debt.contains("AuditService.recordAudit("), debt);
            assertTrue(debt.contains("ExportService.apply("), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
