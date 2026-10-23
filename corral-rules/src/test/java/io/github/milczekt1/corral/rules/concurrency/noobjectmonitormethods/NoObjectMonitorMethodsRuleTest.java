package io.github.milczekt1.corral.rules.concurrency.noobjectmonitormethods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.rules.concurrency.noobjectmonitormethods.fixtures.AlertService;
import io.github.milczekt1.corral.rules.concurrency.noobjectmonitormethods.fixtures.HandoffBuffer;
import io.github.milczekt1.corral.rules.concurrency.noobjectmonitormethods.fixtures.JobBacklog;
import io.github.milczekt1.corral.rules.concurrency.noobjectmonitormethods.fixtures.Notifier;
import io.github.milczekt1.corral.rules.concurrency.noobjectmonitormethods.fixtures.PendingJobs;
import io.github.milczekt1.corral.rules.concurrency.noobjectmonitormethods.fixtures.RetryPacer;
import io.github.milczekt1.corral.rules.concurrency.noobjectmonitormethods.fixtures.ShutdownSignal;
import io.github.milczekt1.corral.rules.concurrency.noobjectmonitormethods.fixtures.StartupGate;
import io.github.milczekt1.corral.rules.concurrency.noobjectmonitormethods.fixtures.WakeupRelay;
import io.github.milczekt1.corral.rules.concurrency.noobjectmonitormethods.fixtures.WorkQueue;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoObjectMonitorMethodsRuleTest {

    private static final String ID = "corral.concurrency.no-object-monitor-methods";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    private static final JavaClasses EXAMPLES = new ClassFileImporter().importClasses(
            WorkQueue.class, RetryPacer.class, JobBacklog.class, Notifier.class, AlertService.class,
            StartupGate.class, HandoffBuffer.class, ShutdownSignal.class, WakeupRelay.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static final List<String> VIOLATIONS = violationsIn(EXAMPLES);

    private static List<String> violationsIn(JavaClasses classes) {
        return NoObjectMonitorMethodsRule.DEFINITION.evaluate(classes).getFailureReport().getDetails();
    }

    private static void assertFlagged(String caller, String target) {
        assertTrue(VIOLATIONS.stream().anyMatch(line -> line.contains(caller + "(") && line.contains(target + ">")),
                String.join("\n", VIOLATIONS));
    }

    private static void assertNotReported(String subject) {
        String report = String.join("\n", VIOLATIONS);

        assertFalse(report.contains(subject), report);
    }

    @Test
    void flagsWaitOnAnObjectField() {
        assertFlagged("WorkQueue.awaitWork", "java.lang.Object.wait()");
    }

    @Test
    void flagsNotifyAllOnAnObjectField() {
        assertFlagged("WorkQueue.submit", "java.lang.Object.notifyAll()");
    }

    @Test
    void flagsATimedWaitOnThis() {
        assertFlagged("RetryPacer.pause", "java.lang.Object.wait(long)");
    }

    @Test
    void flagsATimedWaitWithNanosOnThis() {
        assertFlagged("RetryPacer.pauseBriefly", "java.lang.Object.wait(long, int)");
    }

    @Test
    void flagsNotifyOnThis() {
        assertFlagged("RetryPacer.release", "java.lang.Object.notify()");
    }

    @Test
    void flagsNotifyAllOnAReceiverTypedAsACollection() {
        assertFlagged("JobBacklog.enqueue", "java.lang.Object.notifyAll()");
    }

    /**
     * Kept out of {@code EXAMPLES} and the committed store: the recorded owner is {@code Object} under
     * JDK 17 javac and {@code java.util.Deque} under 18+, so the store line would differ by JDK.
     */
    @Test
    void flagsNotifyAllOnAReceiverTypedAsAnInterface() {
        List<String> violations = violationsIn(new ClassFileImporter().importClasses(PendingJobs.class));

        assertTrue(violations.stream().anyMatch(line -> line.contains("PendingJobs.enqueue(")
                && line.contains(".notifyAll()>")), String.join("\n", violations));
    }

    @Test
    void flagsTimeUnitTimedWait() {
        assertFlagged("ShutdownSignal.awaitStop", "java.util.concurrent.TimeUnit.timedWait(java.lang.Object, long)");
    }

    @Test
    void flagsAMethodReferenceToNotifyAll() {
        assertTrue(VIOLATIONS.stream().anyMatch(line -> line.contains("WakeupRelay.wakeAll(")
                && line.contains("references method <java.lang.Object.notifyAll()>")), String.join("\n", VIOLATIONS));
    }

    @Test
    void reportsOnlyTheMonitorAccesses() {
        assertEquals(8, VIOLATIONS.size(), String.join("\n", VIOLATIONS));
    }

    @Test
    void ignoresTheNoArgumentCallBesideAFlaggedWait() {
        assertNotReported("isEmpty");
    }

    @Test
    void ignoresAnotherTimeUnitCallBesideAFlaggedTimedWait() {
        assertNotReported("toMillis");
    }

    @Test
    void ignoresAnotherMethodReferenceBesideAFlaggedOne() {
        assertNotReported("hashCode");
    }

    @Test
    void ignoresDomainMethodsSharingTheMonitorNames() {
        assertNotReported("AlertService");
    }

    @Test
    void ignoresALatchAwaitedWithATimeout() {
        assertNotReported("StartupGate");
    }

    @Test
    void ignoresAConditionAwaitedAndSignalled() {
        assertNotReported("HandoffBuffer");
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoObjectMonitorMethodsRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(EXAMPLES);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("WorkQueue.awaitWork("), debt);
            assertTrue(debt.contains("RetryPacer.pauseBriefly("), debt);
            assertTrue(debt.contains("JobBacklog.enqueue("), debt);
            assertTrue(debt.contains("ShutdownSignal.awaitStop("), debt);
            assertTrue(debt.contains("WakeupRelay.wakeAll("), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
