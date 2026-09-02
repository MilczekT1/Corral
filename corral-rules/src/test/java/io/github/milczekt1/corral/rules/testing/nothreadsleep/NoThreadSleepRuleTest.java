package io.github.milczekt1.corral.rules.testing.nothreadsleep;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.scope.TestScope;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * The examples are nested and static, so no runner selects them — a top-level {@code *IT} outside a
 * {@code fixtures} package is run by Failsafe for real.
 */
class NoThreadSleepRuleTest {

    /** {@code currentThread} is deliberate: same owner, not a sleep, so an over-broad predicate fails. */
    static class ThreadSleeper {

        void awaitTheCharge() throws InterruptedException {
            Thread.sleep(500);
        }

        String waitingThread() {
            return Thread.currentThread().getName();
        }
    }

    private static final String ID = "corral.test.no-thread-sleep";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    /** {@code TimeUnit} sleeps and is JDK-sourced, so it is production-scoped: it pins the scope clause. */
    private static final JavaClasses EXAMPLES = new ClassFileImporter()
            .importClasses(ThreadSleeper.class, TimeUnit.class);

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

    /** Premises asserted, so a JDK that changed either fails loudly instead of unpinning the clause. */
    @Test
    void ignoresASleepInProductionCode() {
        JavaClass timeUnit = EXAMPLES.get(TimeUnit.class);

        assertFalse(TestScope.TEST_CLASSES.test(timeUnit),
                "premise: a JDK class has no test-output source, so it is production-scoped");
        assertTrue(timeUnit.getMethodCallsFromSelf().stream()
                        .anyMatch(call -> call.getTargetOwner().isEquivalentTo(Thread.class)
                                && call.getName().equals("sleep")),
                "premise: TimeUnit.sleep must still call Thread.sleep, or this pins nothing");

        assertFalse(report().contains("TimeUnit"),
                "this rule is scoped to test classes, and TimeUnit is not one: " + report());
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoThreadSleepRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(EXAMPLES);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("ThreadSleeper"), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
