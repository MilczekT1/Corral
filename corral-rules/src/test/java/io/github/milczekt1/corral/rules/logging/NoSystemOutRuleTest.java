package io.github.milczekt1.corral.rules.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NoSystemOutRuleTest {

    private static final JavaClasses FIXTURES = new ClassFileImporter()
            .importPackages("io.github.milczekt1.corral.fixtures.logging");

    private static String report(ArchRule rule) {
        return String.join("\n", rule.allowEmptyShould(true).evaluate(FIXTURES).getFailureReport().getDetails());
    }

    /**
     * Every shape of write the field match has to catch: a non-String {@code println} overload,
     * {@code printf}, and a write from a static initializer.
     */
    @ParameterizedTest(name = "flags {0}")
    @ValueSource(strings = {"PrintlnIntCaller", "PrintfCaller", "StaticInitializerPrinter"})
    void flagsEveryWayOfWritingToTheStream(String fixture) {
        String report = report(NoSystemOutRule.DEFINITION);

        assertTrue(report.contains(fixture), report);
    }

    /**
     * Every shape the rule must leave alone: neither stream touched, System.err (the sibling rule's),
     * and {@code printStackTrace()} — a known gap, since the JDK touches the field, not the caller.
     */
    @ParameterizedTest(name = "stays silent on {0}")
    @ValueSource(strings = {"SilentComponent", "StderrCaller", "PrintStackTraceCaller"})
    void staysSilentOnClassesTheRuleDoesNotOwn(String fixture) {
        String report = report(NoSystemOutRule.DEFINITION);

        assertFalse(report.contains(fixture), report);
    }

    @Test
    void publicRuleIsFrozenAndIdPinned() {
        assertEquals("corral.logging.no-system-out", NoSystemOutRule.rule.getDescription());
    }
}
