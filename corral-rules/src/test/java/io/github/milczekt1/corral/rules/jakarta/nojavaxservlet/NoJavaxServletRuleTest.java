package io.github.milczekt1.corral.rules.jakarta.nojavaxservlet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.jakarta.nojavaxservlet.fixtures.FilterRegistry;
import io.github.milczekt1.corral.rules.jakarta.nojavaxservlet.fixtures.HalfMigratedAuthFilter;
import io.github.milczekt1.corral.rules.jakarta.nojavaxservlet.fixtures.JakartaAuditFilter;
import io.github.milczekt1.corral.rules.jakarta.nojavaxservlet.fixtures.JavaxTenantFilter;
import io.github.milczekt1.corral.rules.jakarta.nojavaxservlet.fixtures.LegacyRequestLogger;
import io.github.milczekt1.corral.rules.jakarta.nojavaxservlet.fixtures.SocketSupport;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The examples live in {@code fixtures/}, which Surefire and Sonar both exclude: they are
 * deliberately bad code, and a linter told to fix any of it would delete the violation under test.
 */
class NoJavaxServletRuleTest {

    private static final String ID = "corral.jakarta.no-javax-servlet";

    private static final JavaClasses EXAMPLES = new ClassFileImporter().importClasses(
            JavaxTenantFilter.class, LegacyRequestLogger.class, HalfMigratedAuthFilter.class,
            FilterRegistry.class, JakartaAuditFilter.class, SocketSupport.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violations() {
        return NoJavaxServletRule.DEFINITION.evaluate(EXAMPLES).getFailureReport().getDetails();
    }

    private static void assertFlagged(String subject, String target) {
        List<String> violations = violations();

        assertTrue(violations.stream().anyMatch(line -> line.contains(subject) && line.contains(target)),
                String.join("\n", violations));
    }

    @Test
    void flagsAJavaxFilterImplementation() {
        assertFlagged("JavaxTenantFilter", "implements interface <javax.servlet.Filter>");
    }

    @Test
    void flagsAJavaxHttpServletRequestParameter() {
        assertFlagged("LegacyRequestLogger.describe", "parameter of type <javax.servlet.http.HttpServletRequest>");
    }

    @Test
    void flagsADeclaredJavaxServletException() {
        assertFlagged("LegacyRequestLogger.requireTenant", "throws type <javax.servlet.ServletException>");
    }

    @Test
    void flagsAJavaxAnnotationOnAJakartaFilter() {
        assertFlagged("HalfMigratedAuthFilter", "<javax.servlet.annotation.WebFilter>");
    }

    @Test
    void flagsAJavaxTypeArgument() {
        assertFlagged("FilterRegistry.filters", "<javax.servlet.Filter>");
    }

    @Test
    void ignoresJakartaServlet() {
        String report = String.join("\n", violations());

        assertFalse(report.contains("JakartaAuditFilter"), report);
        assertFalse(report.contains("jakarta.servlet"), report);
    }

    @Test
    void ignoresOtherJavaxPackages() {
        String report = String.join("\n", violations());

        assertFalse(report.contains("SocketSupport"), report);
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoJavaxServletRule.rule, ID, EXAMPLES);

        assertTrue(debt.contains("JavaxTenantFilter"), debt);
        assertTrue(debt.contains("LegacyRequestLogger"), debt);
        assertTrue(debt.contains("HalfMigratedAuthFilter"), debt);
        assertTrue(debt.contains("FilterRegistry"), debt);
    }
}
