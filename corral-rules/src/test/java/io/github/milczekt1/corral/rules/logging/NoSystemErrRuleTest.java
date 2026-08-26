package io.github.milczekt1.corral.rules.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class NoSystemErrRuleTest {

    private static final JavaClasses FIXTURES = new ClassFileImporter()
            .importPackages("io.github.milczekt1.corral.fixtures.logging");

    private static String report(ArchRule rule) {
        return String.join("\n", rule.allowEmptyShould(true).evaluate(FIXTURES).getFailureReport().getDetails());
    }

    @Test
    void flagsAnyMethodCalledOnStderr() {
        String report = report(NoSystemErrRule.DEFINITION);

        assertTrue(report.contains("StderrCaller"), report);
    }

    @Test
    void staysSilentOnClassesThatWriteToNeitherStream() {
        String report = report(NoSystemErrRule.DEFINITION);

        assertFalse(report.contains("SilentComponent"), report);
    }

    @Test
    void ignoresStdoutWrites() {
        // The sibling rule owns System.out. Separate ids are separate freeze-store keys, so a
        // consumer's stdout debt must not land under this rule's entry.
        String report = report(NoSystemErrRule.DEFINITION);

        assertFalse(report.contains("PrintlnIntCaller"), report);
        assertFalse(report.contains("PrintfCaller"), report);
        assertFalse(report.contains("StaticInitializerPrinter"), report);
    }

    @Test
    void doesNotSeePrintStackTrace() {
        // Throwable.printStackTrace() writes to System.err from inside the JDK, so the calling
        // class never accesses the field. The doc argues against it anyway; the rule cannot.
        String report = report(NoSystemErrRule.DEFINITION);

        assertFalse(report.contains("PrintStackTraceCaller"), report);
    }

    @Test
    void publicRuleIsFrozenAndIdPinned() {
        assertEquals("corral.logging.no-system-err", NoSystemErrRule.rule.getDescription());
    }
}
