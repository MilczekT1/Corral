package io.github.milczekt1.corral.rules.testing.nothreadsleep;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import com.tngtech.archunit.library.freeze.ViolationStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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

    /**
     * Flagging the example is half the rule: the finding then has to reach the freeze store, filed
     * under this rule's id — the key every consumer's recorded debt lives under, and the reason an
     * id is never renamed. How freezing itself behaves is ArchUnit's, pinned once by
     * {@code DocumentedRuleTest} and {@code EmptyOmittingViolationStoreTest}.
     *
     * <p>The store is a map handed to this one rule by {@link FreezingArchRule#persistIn}; a
     * configured {@code freeze.store} would be process-wide and leak across the Surefire JVM.
     */
    @Test
    void freezesWhatItFindsUnderTheRuleId() {
        InMemoryViolationStore store = new InMemoryViolationStore();
        ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoThreadSleepRule.rule,
                "the published field must be frozen — an unfrozen rule fails on adoption")
                .persistIn(store);

        frozen.check(EXAMPLES);

        List<String> debt = store.violationsFiledUnder("corral.test.no-thread-sleep");
        assertTrue(debt.stream().anyMatch(line -> line.contains("ThreadSleeper")), debt::toString);
        assertTrue(debt.stream().anyMatch(line -> line.contains("TimeUnitSleeper")), debt::toString);
    }

    /** Keyed on the rule description, which {@code guard()} has renamed to the doc id. */
    private static final class InMemoryViolationStore implements ViolationStore {

        private final Map<String, List<String>> violationsByRuleDescription = new HashMap<>();

        List<String> violationsFiledUnder(String ruleId) {
            return violationsByRuleDescription.getOrDefault(ruleId, List.of());
        }

        @Override
        public void initialize(Properties properties) {
            // The map is the store. FreezingArchRule re-initialises whatever it is handed.
        }

        @Override
        public boolean contains(ArchRule rule) {
            return violationsByRuleDescription.containsKey(rule.getDescription());
        }

        @Override
        public void save(ArchRule rule, List<String> violations) {
            violationsByRuleDescription.put(rule.getDescription(), List.copyOf(violations));
        }

        @Override
        public List<String> getViolations(ArchRule rule) {
            return violationsByRuleDescription.getOrDefault(rule.getDescription(), List.of());
        }
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
