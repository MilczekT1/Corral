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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The three classes the rule is aimed at are declared first, and deliberately stay first: the
 * committed freeze store records the <em>line</em> each sleep sits on, so anything inserted above
 * them rewrites the store on an edit that changed nothing about the rule.
 *
 * <p>They are static nested classes, so no runner selects them and they need neither an {@code *IT}
 * name nor a {@code fixtures} package to stay unexecuted — only a compiler, which puts them in test
 * output and therefore in {@code TestScope.TEST_CLASSES}.
 */
class NoThreadSleepRuleTest {

    /** The canonical violation: a guess about how long the charge takes, on someone else's machine. */
    static class ThreadSleeper {

        void awaitTheCharge() throws InterruptedException {
            Thread.sleep(500);
        }
    }

    /** The same wait spelled through TimeUnit — the rewrite the anti-fix guidance warns about. */
    static class TimeUnitSleeper {

        void awaitTheCharge() throws InterruptedException {
            TimeUnit.MILLISECONDS.sleep(500);
        }
    }

    /**
     * The shape howToFix recommends — waits on the condition with a ceiling, and reads the clock —
     * so it must not be flagged. Note what that does <em>not</em> prove: the rule has no notion of a
     * timeout, and ignores this class only because no call here is named {@code sleep}. An
     * {@code await()} with no ceiling would be ignored just the same.
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
            .importClasses(ThreadSleeper.class, TimeUnitSleeper.class, ConditionWaiter.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static String report() {
        return String.join("\n", NoThreadSleepRule.DEFINITION
                .allowEmptyShould(true).evaluate(EXAMPLES).getFailureReport().getDetails());
    }

    @Nested
    class Flags {

        @Test
        void aSleepOnThread() {
            String report = report();

            assertTrue(report.contains("ThreadSleeper"), report);
        }

        @Test
        void aSleepOnTimeUnit() {
            // TimeUnit.sleep delegates to Thread.sleep, so the rewrite must not dodge the match.
            String report = report();

            assertTrue(report.contains("TimeUnitSleeper"), report);
        }
    }

    @Nested
    class Ignores {

        @Test
        void aWaitThatNeverCallsSleep() {
            String report = report();

            assertFalse(report.contains("ConditionWaiter"),
                    "the fix this rule recommends must not itself be flagged: " + report);
        }

        @Test
        void otherMethodsOnThreadAndTimeUnit() {
            // The owner alone must not decide: dropping the name clause reports these two calls.
            String report = report();

            assertFalse(report.contains("currentThread"), report);
            assertFalse(report.contains("toMillis"), report);
        }
    }

    /**
     * Flagging the example is half the rule: the finding then has to reach the freeze store, in a
     * file named for this rule's id — the key every consumer's recorded debt lives under, and the
     * reason an id is never renamed. The store is committed, so what this rule records is reviewed
     * like any other file and a reworded predicate shows up as a diff. Reseed it the way
     * {@code corral-example} documents, then commit the result:
     * {@code ./mvnw test -pl corral-rules -Darchunit.freeze.store.default.allowStoreCreation=true}
     *
     * <p>The store is handed over by {@link FreezingArchRule#persistIn}, not named in
     * {@code freeze.store}: the published field is frozen during class initialisation, which happens
     * at whichever test touches the rule first, so a configured store class races class loading. Its
     * path still goes on the process-wide {@link ArchConfiguration}, reset in a {@code finally},
     * because {@code FreezingArchRule} re-initialises whatever store it is given.
     */
    @Test
    void freezesWhatItFindsIntoTheCommittedStoreUnderTheRuleId() throws IOException {
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
            assertTrue(debt.contains("TimeUnitSleeper"), debt);
            assertFalse(debt.contains("ConditionWaiter"), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
