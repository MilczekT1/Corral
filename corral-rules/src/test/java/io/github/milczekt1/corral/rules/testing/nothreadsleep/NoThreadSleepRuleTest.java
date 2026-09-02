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
 * The class under examination comes first and stays first: the committed store records the line its
 * violation sits on, so anything inserted above it rewrites the store on an unrelated edit.
 *
 * <p>It is a static nested class, so no runner selects it — no {@code *IT} name and no
 * {@code fixtures} package needed, only a compiler, which puts it in test output and therefore in
 * {@code TestScope.TEST_CLASSES}.
 *
 * <p>There is no second, deliberately-ignored class. The store is the negative direction instead:
 * {@code check} fails on any violation the file does not already record, so a widened predicate
 * fails here rather than passing quietly. That only works because the example holds a call the rule
 * must <em>not</em> match — with a single matching call and nothing else, every over-broad
 * predicate finds exactly the recorded violation and stays green.
 */
class NoThreadSleepRuleTest {

    /**
     * The sleep is a guess about how long the charge takes, on someone else's machine, and must be
     * flagged. The {@code currentThread} call is on the same owner and must not be — the store
     * recording one line and not two is what pins both directions from a single example.
     */
    static class ThreadSleeper {

        void awaitTheCharge() throws InterruptedException {
            Thread.sleep(500);
        }

        String waitingThread() {
            return Thread.currentThread().getName();
        }
    }

    private static final String ID = "corral.test.no-thread-sleep";

    /** Committed, and resolved against the JVM's working directory — the module, under Surefire. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    /**
     * {@code TimeUnit} is imported from the JDK, so it is production-scoped, and its {@code sleep}
     * delegates to {@code Thread.sleep} — which makes it the only thing here that can pin the
     * {@code TEST_CLASSES} clause. A test cannot declare a class outside test scope.
     */
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

    /** Both premises asserted, so a JDK that changed either fails loudly rather than unpinning. */
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
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
