package io.github.milczekt1.corral.rules.concurrency.nomanualthreadcreation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.concurrency.nomanualthreadcreation.fixtures.RaceProvoker;
import io.github.milczekt1.corral.rules.concurrency.nomanualthreadcreation.fixtures.production.ForkJoinCounterThread;
import io.github.milczekt1.corral.rules.concurrency.nomanualthreadcreation.fixtures.production.PollingWorker;
import io.github.milczekt1.corral.rules.concurrency.nomanualthreadcreation.fixtures.production.SweepTimer;
import io.github.milczekt1.corral.rules.concurrency.nomanualthreadcreation.fixtures.production.WorkerPool;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import io.github.milczekt1.corral.rules.testsupport.FixtureVerdicts;
import io.github.milczekt1.corral.rules.testsupport.ProductionLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NoManualThreadCreationRuleTest {

    private static final String ID = "corral.concurrency.no-manual-thread-creation";

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        examples = ProductionLayout.importFixtures(NoManualThreadCreationRuleTest.class, outputRoot.resolve("examples"));
    }

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violationsIn(JavaClasses classes) {
        return NoManualThreadCreationRule.DEFINITION.evaluate(classes).getFailureReport().getDetails();
    }

    private static void assertFlagged(String caller, String access) {
        List<String> violations = violationsIn(examples);

        assertTrue(violations.stream().anyMatch(line -> line.contains(caller + "(") && line.contains(access)),
                String.join("\n", violations));
    }

    private static void assertNotReported(String subject) {
        String report = String.join("\n", violationsIn(examples));

        assertFalse(report.contains(subject), report);
    }

    @Test
    void flagsAThreadConstructedWithATask() {
        assertFlagged("OrderDispatcher.placeAsync",
                "calls constructor <java.lang.Thread.<init>(java.lang.Runnable)>");
    }

    @Test
    void flagsAThreadConstructedWithATaskAndAName() {
        assertFlagged("OrderDispatcher.placeNamed",
                "calls constructor <java.lang.Thread.<init>(java.lang.Runnable, java.lang.String)>");
    }

    @Test
    void flagsATimer() {
        assertFlagged("ExpiryScheduler.expireLater", "calls constructor <java.util.Timer.<init>()>");
    }

    @Test
    void flagsANamedDaemonTimer() {
        assertFlagged("ExpiryScheduler.reaper",
                "calls constructor <java.util.Timer.<init>(java.lang.String, boolean)>");
    }

    @Test
    void flagsAConstructionOfAProjectsOwnTimerSubclass() {
        assertFlagged("ExpiryScheduler.sweeper", "calls constructor <" + SweepTimer.class.getName() + ".<init>()>");
    }

    @Test
    void flagsATimerSubclassWhereItCallsTimersConstructor() {
        assertFlagged(SweepTimer.class.getName() + ".<init>",
                "calls constructor <java.util.Timer.<init>(java.lang.String, boolean)>");
    }

    @Test
    void flagsASubclassWhereItCallsThreadsConstructor() {
        assertFlagged(PollingWorker.class.getName() + ".<init>", "calls constructor <java.lang.Thread.<init>()>");
    }

    @Test
    void flagsAConstructionOfAProjectsOwnThreadSubclass() {
        assertFlagged("WorkerPool.startPolling", "calls constructor <" + PollingWorker.class.getName() + ".<init>()>");
    }

    @Test
    void flagsAnAnonymousThreadWhereItIsConstructed() {
        assertFlagged("WorkerPool.startHeartbeat",
                "calls constructor <" + WorkerPool.class.getName() + "$1.<init>(");
    }

    @Test
    void flagsAnAnonymousThreadWhereItCallsThreadsConstructor() {
        assertFlagged(WorkerPool.class.getName() + "$1.<init>", "calls constructor <java.lang.Thread.<init>()>");
    }

    @Test
    void flagsAThreadConstructorReference() {
        assertFlagged("WorkerPool.pool", "references constructor <java.lang.Thread.<init>(java.lang.Runnable)>");
    }

    @Test
    void flagsAHandRolledThreadFactory() {
        assertFlagged("HandRolledThreadFactory.newThread",
                "calls constructor <java.lang.Thread.<init>(java.lang.Runnable, java.lang.String)>");
    }

    @Test
    void flagsASubclassOfAJdkThreadSubclass() {
        assertFlagged(ForkJoinCounterThread.class.getName() + ".<init>",
                "calls constructor <java.util.concurrent.ForkJoinWorkerThread.<init>("
                        + "java.util.concurrent.ForkJoinPool)>");
    }

    @Test
    void reportsExactlyThirteenConstructions() {
        assertEquals(13, violationsIn(examples).size(), String.join("\n", violationsIn(examples)));
    }

    @Test
    void ignoresAThreadMethodAndAnotherConstructorBesideFlaggedThreads() {
        assertNotReported("OrderDispatcher.describeCaller(");
    }

    @Test
    void ignoresATimerTaskAndASwingTimerBesideFlaggedTimers() {
        assertNotReported("ExpiryScheduler$1");
        assertNotReported("ExpiryScheduler.blinker(");
    }

    @Test
    void ignoresAPoolFactoryMethodBesideAFlaggedReference() {
        assertNotReported("newFixedThreadPool");
    }

    @Test
    void ignoresAnInjectedExecutorAndAThreadParameter() {
        assertNotReported("PaymentService");
    }

    @Test
    void ignoresAThreadFactoryDelegatingToTheJdkFactory() {
        assertNotReported("DelegatingThreadFactory");
    }

    @Test
    void ignoresAThreadConstructedInTestOutput() {
        assertNotReported("RaceProvoker");
    }

    /** The same class file as above, moved: only its location changed, so only scope can explain the difference. */
    @Test
    void flagsTheTestOutputRaceProvokerOnceItSitsInProductionOutput() throws IOException {
        Path productionOutput = ProductionLayout.copy(outputRoot.resolve("provoker"), List.of(RaceProvoker.class));
        JavaClasses provoker = new ClassFileImporter().importPath(productionOutput);

        List<String> violations = violationsIn(provoker);

        assertTrue(violations.stream().anyMatch(line -> line.contains("RaceProvoker.contender(")),
                String.join("\n", violations));
    }

    @Test
    void everyFixtureMatchesItsVerdict() {
        FixtureVerdicts.assertMatch(NoManualThreadCreationRuleTest.class, NoManualThreadCreationRule.DEFINITION, examples);
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoManualThreadCreationRule.rule, ID, examples);

        assertTrue(debt.contains("OrderDispatcher.placeAsync("), debt);
        assertTrue(debt.contains("OrderDispatcher.placeNamed("), debt);
        assertTrue(debt.contains("ExpiryScheduler.expireLater("), debt);
        assertTrue(debt.contains("ExpiryScheduler.reaper("), debt);
        assertTrue(debt.contains("ExpiryScheduler.sweeper("), debt);
        assertTrue(debt.contains("SweepTimer.<init>()> calls"), debt);
        assertTrue(debt.contains("PollingWorker.<init>()> calls"), debt);
        assertTrue(debt.contains("WorkerPool.startPolling("), debt);
        assertTrue(debt.contains("WorkerPool.startHeartbeat("), debt);
        assertTrue(debt.contains("WorkerPool$1.<init>(" + WorkerPool.class.getName() + ")> calls"), debt);
        assertTrue(debt.contains("WorkerPool.pool("), debt);
        assertTrue(debt.contains("HandRolledThreadFactory.newThread("), debt);
        assertTrue(debt.contains("ForkJoinCounterThread.<init>("), debt);
    }
}
