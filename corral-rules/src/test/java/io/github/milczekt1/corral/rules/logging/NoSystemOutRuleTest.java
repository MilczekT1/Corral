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
     * Every shape of write the field match has to catch:
     *
     * <ul>
     *   <li>{@code PrintlnIntCaller} — the gap that motivated the rule: matching
     *       {@code callMethod(println, String.class)} misses this overload entirely.
     *   <li>{@code PrintfCaller} — printf and print are not println at all; only the field access
     *       reaches them.
     *   <li>{@code StaticInitializerPrinter} — a write from a static initializer, which the field
     *       match reports like any other code location.
     * </ul>
     */
    @ParameterizedTest(name = "flags {0}")
    @ValueSource(strings = {"PrintlnIntCaller", "PrintfCaller", "StaticInitializerPrinter"})
    void flagsEveryWayOfWritingToTheStream(String fixture) {
        String report = report(NoSystemOutRule.DEFINITION);

        assertTrue(report.contains(fixture), report);
    }

    /**
     * Every shape the rule must leave alone:
     *
     * <ul>
     *   <li>{@code SilentComponent} — touches neither stream.
     *   <li>{@code StderrCaller} — the sibling rule owns System.err. Two ids mean two freeze-store
     *       keys, so overlap here would record the same debt twice.
     *   <li>{@code PrintStackTraceCaller} — known and accepted: {@code Throwable.printStackTrace()}
     *       touches the field from inside the JDK, so the calling class never accesses it. Pinned so
     *       the gap is a decision, not a surprise.
     * </ul>
     */
    @ParameterizedTest(name = "stays silent on {0}")
    @ValueSource(strings = {"SilentComponent", "StderrCaller", "PrintStackTraceCaller"})
    void staysSilentOnClassesTheRuleDoesNotOwn(String fixture) {
        String report = report(NoSystemOutRule.DEFINITION);

        assertFalse(report.contains(fixture), report);
    }

    @Test
    void publicRuleIsFrozenAndIdPinned() {
        assertEquals("logging.no-system-out", NoSystemOutRule.rule.getDescription());
    }
}
