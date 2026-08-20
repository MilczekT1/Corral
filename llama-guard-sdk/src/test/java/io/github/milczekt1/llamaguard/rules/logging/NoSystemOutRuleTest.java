package io.github.milczekt1.llamaguard.rules.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class NoSystemOutRuleTest {

    private static final JavaClasses FIXTURES = new ClassFileImporter()
            .importPackages("io.github.milczekt1.llamaguard.fixtures.logging");

    private static String report(ArchRule rule) {
        return String.join("\n", rule.allowEmptyShould(true).evaluate(FIXTURES).getFailureReport().getDetails());
    }

    @Test
    void flagsOverloadsOfPrintlnOtherThanTheStringOne() {
        // The gap that motivated the rule: matching callMethod(println, String.class) misses this.
        String report = report(NoSystemOutRule.RULE);

        assertTrue(report.contains("PrintlnIntCaller"), report);
    }

    @Test
    void flagsEveryOtherWayOfWritingToTheStream() {
        // printf and print are not println at all; the field access is what catches them.
        String report = report(NoSystemOutRule.RULE);

        assertTrue(report.contains("PrintfCaller"), report);
    }

    @Test
    void flagsWritesFromAStaticInitializer() {
        String report = report(NoSystemOutRule.RULE);

        assertTrue(report.contains("StaticInitializerPrinter"), report);
    }

    @Test
    void staysSilentOnClassesThatWriteToNeitherStream() {
        String report = report(NoSystemOutRule.RULE);

        assertFalse(report.contains("SilentComponent"), report);
    }

    @Test
    void ignoresStderrWrites() {
        // The sibling rule owns System.err. Two ids mean two freeze-store keys, so overlap here
        // would record the same debt twice.
        String report = report(NoSystemOutRule.RULE);

        assertFalse(report.contains("StderrCaller"), report);
    }

    @Test
    void doesNotSeePrintStackTrace() {
        // Known and accepted: Throwable.printStackTrace() touches the field from inside the JDK,
        // so the calling class never accesses it. Pinned so the gap is a decision, not a surprise.
        String report = report(NoSystemOutRule.RULE);

        assertFalse(report.contains("PrintStackTraceCaller"), report);
    }

    @Test
    void publicRuleIsFrozenAndIdPinned() {
        assertEquals("logging.no-system-out", NoSystemOutRule.rule.getDescription());
    }
}
