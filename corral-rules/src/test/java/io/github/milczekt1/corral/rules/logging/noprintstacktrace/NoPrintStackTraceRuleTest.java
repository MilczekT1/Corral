package io.github.milczekt1.corral.rules.logging.noprintstacktrace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.rules.logging.noprintstacktrace.fixtures.BatchRunner;
import io.github.milczekt1.corral.rules.logging.noprintstacktrace.fixtures.ChargeService;
import io.github.milczekt1.corral.rules.logging.noprintstacktrace.fixtures.ConsoleDumper;
import io.github.milczekt1.corral.rules.logging.noprintstacktrace.fixtures.DiagnosticsPanel;
import io.github.milczekt1.corral.rules.logging.noprintstacktrace.fixtures.FailureListener;
import io.github.milczekt1.corral.rules.logging.noprintstacktrace.fixtures.FrameInspector;
import io.github.milczekt1.corral.rules.logging.noprintstacktrace.fixtures.OrderSubmitter;
import io.github.milczekt1.corral.rules.logging.noprintstacktrace.fixtures.ReportWriter;
import io.github.milczekt1.corral.rules.logging.noprintstacktrace.fixtures.SettlementException;
import io.github.milczekt1.corral.rules.logging.noprintstacktrace.fixtures.SettlementJob;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoPrintStackTraceRuleTest {

    private static final String ID = "corral.logging.no-print-stack-trace";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    private static final JavaClasses EXAMPLES = new ClassFileImporter().importClasses(
            OrderSubmitter.class, ConsoleDumper.class, ReportWriter.class, FailureListener.class,
            BatchRunner.class, SettlementJob.class, SettlementException.class, ChargeService.class,
            FrameInspector.class, DiagnosticsPanel.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static final List<String> VIOLATIONS =
            NoPrintStackTraceRule.DEFINITION.evaluate(EXAMPLES).getFailureReport().getDetails();

    private static void assertFlagged(String caller, String target) {
        assertTrue(VIOLATIONS.stream().anyMatch(line -> line.contains(caller) && line.contains(target + ">")),
                String.join("\n", VIOLATIONS));
    }

    private static void assertNotReported(String subject) {
        String report = String.join("\n", VIOLATIONS);

        assertFalse(report.contains(subject), report);
    }

    @Test
    void flagsPrintStackTraceInACatchBlock() {
        assertFlagged("OrderSubmitter.submit(", "calls method <java.io.IOException.printStackTrace()");
    }

    @Test
    void flagsThePrintStreamOverload() {
        assertFlagged("ConsoleDumper.dump(",
                "calls method <java.lang.IllegalStateException.printStackTrace(java.io.PrintStream)");
    }

    @Test
    void flagsThePrintWriterOverload() {
        assertFlagged("ReportWriter.render(", "calls method <java.lang.Exception.printStackTrace(java.io.PrintWriter)");
    }

    @Test
    void flagsAMethodReference() {
        assertFlagged("FailureListener.onFailures(", "references method <java.lang.Throwable.printStackTrace()");
    }

    @Test
    void flagsACallInsideALambdaAgainstTheEnclosingMethod() {
        assertFlagged("BatchRunner.runAll(", "calls method <java.lang.RuntimeException.printStackTrace()");
    }

    @Test
    void flagsACallOnAProjectExceptionSubtype() {
        assertFlagged("SettlementJob.settle(", "printStackTrace()");
    }

    @Test
    void reportsOnlyThePrintStackTraceAccesses() {
        assertEquals(6, VIOLATIONS.size(), String.join("\n", VIOLATIONS));
    }

    @Test
    void ignoresAnotherCallOnTheSameExceptionBesideAFlaggedOne() {
        assertNotReported("getMessage");
    }

    @Test
    void ignoresTheStreamAndWriterCallsBesideAFlaggedOverload() {
        assertNotReported("flush");
        assertNotReported("java.io.PrintWriter.<init>");
    }

    @Test
    void ignoresAPrintPrefixedMethodOnAThrowableThatIsNotPrintStackTrace() {
        assertNotReported("printableSummary");
    }

    @Test
    void ignoresAThrowableHandedToALogger() {
        assertNotReported("ChargeService");
    }

    @Test
    void ignoresReadingTheFramesWithoutPrintingThem() {
        assertNotReported("FrameInspector");
    }

    @Test
    void ignoresAMethodNamedPrintStackTraceOnANonThrowable() {
        assertNotReported("DiagnosticsPanel");
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoPrintStackTraceRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(EXAMPLES);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("OrderSubmitter.submit("), debt);
            assertTrue(debt.contains("ConsoleDumper.dump("), debt);
            assertTrue(debt.contains("ReportWriter.render("), debt);
            assertTrue(debt.contains("FailureListener.onFailures("), debt);
            assertTrue(debt.contains("BatchRunner.runAll("), debt);
            assertTrue(debt.contains("SettlementJob.settle("), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
