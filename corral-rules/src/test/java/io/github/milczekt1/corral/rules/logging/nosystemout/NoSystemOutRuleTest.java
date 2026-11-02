package io.github.milczekt1.corral.rules.logging.nosystemout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.logging.nosystemout.fixtures.PrintfCaller;
import io.github.milczekt1.corral.rules.logging.nosystemout.fixtures.PrintlnIntCaller;
import io.github.milczekt1.corral.rules.logging.nosystemout.fixtures.SilentComponent;
import io.github.milczekt1.corral.rules.logging.nosystemout.fixtures.StaticInitializerPrinter;
import io.github.milczekt1.corral.rules.logging.nosystemout.fixtures.StderrCaller;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import io.github.milczekt1.corral.rules.testsupport.FixtureVerdicts;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NoSystemOutRuleTest {

    private static final String ID = "corral.logging.no-system-out";

    private static final JavaClasses EXAMPLES = new ClassFileImporter().importClasses(
            PrintlnIntCaller.class, PrintfCaller.class, StaticInitializerPrinter.class,
            SilentComponent.class, StderrCaller.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static String report() {
        return String.join("\n", NoSystemOutRule.DEFINITION
                .allowEmptyShould(true).evaluate(EXAMPLES).getFailureReport().getDetails());
    }

    @Test
    void flagsAPrintlnOverloadOtherThanString() {
        String report = report();

        assertTrue(report.contains("PrintlnIntCaller"), report);
    }

    @Test
    void flagsPrintfAndPrint() {
        String report = report();

        assertTrue(report.contains("(PrintfCaller.java:7)"), "printf: " + report);
        assertTrue(report.contains("(PrintfCaller.java:8)"), "print: " + report);
    }

    @Test
    void flagsAWriteFromAStaticInitializer() {
        String report = report();

        assertTrue(report.contains("StaticInitializerPrinter.<clinit>()"), report);
    }

    @ParameterizedTest(name = "ignores {0}")
    @ValueSource(strings = {"SilentComponent", "StderrCaller"})
    void ignoresClassesThatNeverTouchTheStdoutField(String example) {
        String report = report();

        assertFalse(report.contains(example), report);
    }

    @Test
    void everyFixtureMatchesItsVerdict() {
        FixtureVerdicts.assertMatch(NoSystemOutRuleTest.class, NoSystemOutRule.DEFINITION, EXAMPLES);
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoSystemOutRule.rule, ID, EXAMPLES);

        assertTrue(debt.contains("PrintlnIntCaller"), debt);
        assertFalse(debt.contains("StderrCaller"), "a stderr write must never reach this rule's store: " + debt);
    }
}
