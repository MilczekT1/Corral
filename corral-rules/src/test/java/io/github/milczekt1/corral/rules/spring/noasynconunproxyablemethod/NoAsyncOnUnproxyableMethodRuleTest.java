package io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod.fixtures.AsyncWorkerBase;
import io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod.fixtures.AuditService;
import io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod.fixtures.BackgroundTask;
import io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod.fixtures.CourierDispatcher;
import io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod.fixtures.ExportService;
import io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod.fixtures.MailerService;
import io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod.fixtures.OrderService;
import io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod.fixtures.PaymentService;
import io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod.fixtures.ReportService;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import io.github.milczekt1.corral.rules.testsupport.FixtureVerdicts;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoAsyncOnUnproxyableMethodRuleTest {

    private static final String ID = "corral.spring.no-async-on-unproxyable-method";

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

    /** The reason is frozen text: removing a redundant {@code final} must not reword the line. */
    @Test
    void reportsAFinalMethodInAFinalClassAsFinal() {
        assertFlagged("MailerService.archiveDigest", "is final");
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
        assertEquals(13, violations().size(), String.join("\n", violations()));
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

    @Test
    void everyFixtureMatchesItsVerdict() {
        FixtureVerdicts.assertMatch(NoAsyncOnUnproxyableMethodRuleTest.class, NoAsyncOnUnproxyableMethodRule.DEFINITION, EXAMPLES);
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoAsyncOnUnproxyableMethodRule.rule, ID, EXAMPLES);

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
        assertTrue(debt.contains("MailerService.archiveDigest("), debt);
        assertTrue(debt.contains("AuditService.recordAudit("), debt);
        assertTrue(debt.contains("ExportService.apply("), debt);
    }
}
