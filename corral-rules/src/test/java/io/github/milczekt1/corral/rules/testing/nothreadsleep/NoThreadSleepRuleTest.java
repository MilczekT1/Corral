package io.github.milczekt1.corral.rules.testing.nothreadsleep;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * The two classes under examination come first and stay first: the committed store records the line
 * each violation sits on, so anything inserted above them rewrites it on an unrelated edit.
 *
 * <p>They are static nested classes, so no runner selects them — no {@code *IT} name and no
 * {@code fixtures} package needed, only a compiler, which puts them in test output and therefore in
 * {@code TestScope.TEST_CLASSES}.
 */
class NoThreadSleepRuleTest {

    /** Must be flagged: a guess about how long the charge takes, on someone else's machine. */
    static class ThreadSleeper {

        void awaitTheCharge() throws InterruptedException {
            Thread.sleep(500);
        }
    }

    /**
     * Must not be flagged: the shape {@code howToFix} recommends. Its {@code Thread.currentThread}
     * call is what pins the name clause — drop that clause from the predicate and the {@code Thread}
     * owner alone reports this class.
     */
    static class ConditionWaiter {

        private final CountDownLatch charged = new CountDownLatch(1);

        boolean awaitTheCharge() throws InterruptedException {
            return charged.await(TimeUnit.SECONDS.toMillis(5), TimeUnit.MILLISECONDS);
        }

        String waitingThread() {
            return Thread.currentThread().getName();
        }
    }

    private static final String ID = "corral.test.no-thread-sleep";

    /** Committed, and resolved against the JVM's working directory — the module, under Surefire. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    private static final JavaClasses EXAMPLES = new ClassFileImporter()
            .importClasses(ThreadSleeper.class, ConditionWaiter.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static String report() {
        return String.join("\n", NoThreadSleepRule.DEFINITION
                .allowEmptyShould(true).evaluate(EXAMPLES).getFailureReport().getDetails());
    }

    @Test
    void flagsASleepOnThread() {
        String report = report();

        assertTrue(report.contains("ThreadSleeper"), report);
    }

    @Test
    void ignoresAWaitThatNeverCallsSleep() {
        String report = report();

        assertFalse(report.contains("ConditionWaiter"),
                "the fix this rule recommends must not itself be flagged: " + report);
    }

    /**
     * The finding also has to reach the committed store, in a file named for the rule's id — the key
     * every consumer's debt is filed under. Committing it makes a widened predicate fail the build
     * rather than quietly join the accepted set. Reseed, then commit the result:
     * {@code ./mvnw test -pl corral-rules -Darchunit.freeze.store.default.allowStoreCreation=true}
     *
     * <p>The store goes in through {@link FreezingArchRule#persistIn}, not {@code freeze.store}: a
     * frozen rule captures its store when constructed, which is class initialisation, so naming one
     * here races class loading. Only the path goes on {@link ArchConfiguration}, reset in a
     * {@code finally}.
     */
    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        ArchConfiguration.get().setProperty("freeze.store.default.path", STORE_PATH);
        try {
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoThreadSleepRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(EXAMPLES);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("ThreadSleeper"), debt);
            assertFalse(debt.contains("ConditionWaiter"), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
