package io.github.milczekt1.corral.rules.testing.nothreadsleep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The three classes the rule is aimed at are declared at the bottom of this file.
 *
 * <p>They are static nested classes, so no runner selects them and they need neither an {@code *IT}
 * name nor a {@code fixtures} package to stay unexecuted — only a compiler, which puts them in test
 * output and therefore in {@code TestScope.TEST_CLASSES}.
 */
class NoThreadSleepRuleTest {

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
        void aWaitOnAConditionWithACeiling() {
            String report = report();

            assertFalse(report.contains("ConditionWaiter"),
                    "await(timeout) waits on the condition, not on the clock: " + report);
        }

        @Test
        void otherMethodsOnThreadAndTimeUnit() {
            // The owner alone must not decide: dropping the name clause reports these two calls.
            String report = report();

            assertFalse(report.contains("currentThread"), report);
            assertFalse(report.contains("toMillis"), report);
        }
    }

    @Test
    void publicRuleIsFrozenAndIdPinned() {
        assertEquals("corral.test.no-thread-sleep", NoThreadSleepRule.rule.getDescription());
    }

    // --- the classes under examination -------------------------------------------------------

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

    /** Waits on the condition with a ceiling, and calls Thread and TimeUnit without sleeping. */
    static class ConditionWaiter {

        private final CountDownLatch charged = new CountDownLatch(1);

        boolean awaitTheCharge() throws InterruptedException {
            return charged.await(TimeUnit.SECONDS.toMillis(5), TimeUnit.MILLISECONDS);
        }

        String waitingThread() {
            return Thread.currentThread().getName();
        }
    }
}
