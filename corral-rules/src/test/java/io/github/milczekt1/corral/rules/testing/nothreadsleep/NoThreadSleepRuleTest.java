package io.github.milczekt1.corral.rules.testing.nothreadsleep;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.testing.nothreadsleep.fixtures.ThreadSleeper;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import io.github.milczekt1.corral.scope.TestScope;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class NoThreadSleepRuleTest {

    private static final String ID = "corral.test.no-thread-sleep";

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

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoThreadSleepRule.rule, ID, EXAMPLES);

        assertTrue(debt.contains("ThreadSleeper"), debt);
    }
}
