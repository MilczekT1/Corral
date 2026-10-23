package io.github.milczekt1.corral.rules.concurrency.nomanualthreadcreation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.rules.concurrency.nomanualthreadcreation.fixtures.DelegatingThreadFactory;
import io.github.milczekt1.corral.rules.concurrency.nomanualthreadcreation.fixtures.ExpiryScheduler;
import io.github.milczekt1.corral.rules.concurrency.nomanualthreadcreation.fixtures.ForkJoinCounterThread;
import io.github.milczekt1.corral.rules.concurrency.nomanualthreadcreation.fixtures.HandRolledThreadFactory;
import io.github.milczekt1.corral.rules.concurrency.nomanualthreadcreation.fixtures.OrderDispatcher;
import io.github.milczekt1.corral.rules.concurrency.nomanualthreadcreation.fixtures.PaymentService;
import io.github.milczekt1.corral.rules.concurrency.nomanualthreadcreation.fixtures.PollingWorker;
import io.github.milczekt1.corral.rules.concurrency.nomanualthreadcreation.fixtures.RaceProvoker;
import io.github.milczekt1.corral.rules.concurrency.nomanualthreadcreation.fixtures.SweepTimer;
import io.github.milczekt1.corral.rules.concurrency.nomanualthreadcreation.fixtures.WorkerPool;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The rule is scoped to production classes, and every fixture compiles into test output. So the
 * examples are copied into a Gradle main-output layout and imported from there;
 * {@link RaceProvoker} alone is imported from where it compiled.
 */
class NoManualThreadCreationRuleTest {

    private static final String ID = "corral.concurrency.no-manual-thread-creation";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    /** Matched by none of {@code TestScope}'s test-output patterns. */
    private static final String PRODUCTION_LAYOUT = "build/classes/java/main";

    private static final List<Class<?>> PRODUCTION_EXAMPLES = List.of(
            OrderDispatcher.class, ExpiryScheduler.class, PollingWorker.class, WorkerPool.class,
            HandRolledThreadFactory.class, ForkJoinCounterThread.class, SweepTimer.class,
            PaymentService.class, DelegatingThreadFactory.class);

    /** {@link ExpiryScheduler}'s anonymous {@code TimerTask} and {@link WorkerPool}'s anonymous {@code Thread}. */
    private static final int ANONYMOUS_EXAMPLES = 2;

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        Path productionOutput = copyIntoProductionLayout(outputRoot.resolve("examples"), PRODUCTION_EXAMPLES);
        examples = new ClassFileImporter().importLocations(List.of(
                Location.of(productionOutput),
                Location.of(classFileOf(RaceProvoker.class))));

        assertEquals(PRODUCTION_EXAMPLES.size() + ANONYMOUS_EXAMPLES + 1, examples.size(),
                "every example must be imported, or the ignore assertions below are vacuous");
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
        Path productionOutput = copyIntoProductionLayout(outputRoot.resolve("provoker"), List.of(RaceProvoker.class));
        JavaClasses provoker = new ClassFileImporter().importPath(productionOutput);

        List<String> violations = violationsIn(provoker);

        assertTrue(violations.stream().anyMatch(line -> line.contains("RaceProvoker.contender(")),
                String.join("\n", violations));
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoManualThreadCreationRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(examples);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
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
        } finally {
            ArchConfiguration.get().reset();
        }
    }

    /**
     * Copies each class file, with its nested and anonymous classes, into
     * {@code root/}{@value #PRODUCTION_LAYOUT}, keeping package directories.
     */
    private static Path copyIntoProductionLayout(Path root, List<Class<?>> classes) throws IOException {
        Path output = root.resolve(PRODUCTION_LAYOUT);
        for (Class<?> type : classes) {
            Path compiled = pathOf(classFileOf(type));
            Path destination = output.resolve(type.getPackageName().replace('.', '/'));
            Files.createDirectories(destination);
            try (Stream<Path> siblings = Files.list(compiled.getParent())) {
                for (Path classFile : siblings.filter(sibling -> belongsTo(sibling, type)).toList()) {
                    Files.copy(classFile, destination.resolve(classFile.getFileName()));
                }
            }
        }
        return output;
    }

    private static boolean belongsTo(Path classFile, Class<?> type) {
        String name = classFile.getFileName().toString();
        return name.equals(type.getSimpleName() + ".class") || name.startsWith(type.getSimpleName() + "$");
    }

    private static Path pathOf(URL classFile) {
        try {
            return Path.of(classFile.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(classFile.toString(), e);
        }
    }

    private static URL classFileOf(Class<?> type) {
        URL classFile = type.getResource(type.getSimpleName() + ".class");
        assertNotNull(classFile, () -> "no compiled class file on the classpath for " + type.getName());
        return classFile;
    }
}
