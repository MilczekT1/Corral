package io.github.milczekt1.corral.rules.logging.nosystemerr;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.logging.nosystemerr.fixtures.PrintStackTraceCaller;
import io.github.milczekt1.corral.rules.logging.nosystemerr.fixtures.SilentComponent;
import io.github.milczekt1.corral.rules.logging.nosystemerr.fixtures.StderrCaller;
import io.github.milczekt1.corral.rules.logging.nosystemerr.fixtures.StdoutCaller;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class NoSystemErrRuleTest {

    private static final String ID = "corral.logging.no-system-err";

    private static final JavaClasses EXAMPLES = new ClassFileImporter().importClasses(
            StderrCaller.class, SilentComponent.class, StdoutCaller.class, PrintStackTraceCaller.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static String report() {
        return String.join("\n", NoSystemErrRule.DEFINITION
                .allowEmptyShould(true).evaluate(EXAMPLES).getFailureReport().getDetails());
    }

    @Test
    void flagsAnyMethodCalledOnStderr() {
        String report = report();

        assertTrue(report.contains("StderrCaller"), report);
    }

    @Test
    void ignoresAClassTouchingNeitherStream() {
        String report = report();

        assertFalse(report.contains("SilentComponent"), report);
    }

    @Test
    void ignoresStdoutWrites() {
        String report = report();

        assertFalse(report.contains("StdoutCaller"), report);
    }

    @Test
    void doesNotSeePrintStackTrace() {
        String report = report();

        assertFalse(report.contains("PrintStackTraceCaller"), report);
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoSystemErrRule.rule, ID, EXAMPLES);

        assertTrue(debt.contains("StderrCaller"), debt);
        assertFalse(debt.contains("StdoutCaller"), "a stdout write must never reach this rule's store: " + debt);
    }
}
