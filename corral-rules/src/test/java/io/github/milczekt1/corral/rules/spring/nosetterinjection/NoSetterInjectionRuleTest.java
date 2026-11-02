package io.github.milczekt1.corral.rules.spring.nosetterinjection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures.TestOutputWiredHelper;
import io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures.production.InvoiceService;
import io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures.production.LegacyAuditService;
import io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures.production.LegacyBillingService;
import io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures.production.OrderService;
import io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures.production.PaymentService;
import io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures.production.ReportScheduler;
import io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures.production.RepositoryConfiguration;
import io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures.production.ShippingService;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import io.github.milczekt1.corral.rules.testsupport.ProductionLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * ArchUnit reports an annotation by simple name, so {@code jakarta} and {@code javax} read the same:
 * each example carries exactly one of the matched annotations, and the flagged method names which.
 */
class NoSetterInjectionRuleTest {

    private static final String ID = "corral.spring.no-setter-injection";

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        examples = ProductionLayout.importFixtures(NoSetterInjectionRuleTest.class, outputRoot.resolve("examples"));
    }

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violationsIn(JavaClasses classes) {
        return NoSetterInjectionRule.DEFINITION.evaluate(classes).getFailureReport().getDetails();
    }

    private static void assertFlagged(String method, String annotation) {
        List<String> violations = violationsIn(examples);

        assertTrue(violations.stream().anyMatch(line -> line.contains(method) && line.contains(annotation)),
                String.join("\n", violations));
    }

    private static void assertNotReported(String subject) {
        String report = String.join("\n", violationsIn(examples));

        assertFalse(report.contains(subject), report);
    }

    @Test
    void flagsAnAutowiredSetter() {
        assertFlagged("OrderService.setRepository", "@Autowired");
    }

    @Test
    void flagsAJakartaInjectSetter() {
        assertFlagged("InvoiceService.setRepository", "@Inject");
    }

    @Test
    void flagsAJakartaResourceSetter() {
        assertFlagged("ShippingService.setRepository", "@Resource");
    }

    @Test
    void flagsAJavaxInjectSetter() {
        assertFlagged("LegacyAuditService.setRepository", "@Inject");
    }

    @Test
    void flagsAJavaxResourceSetter() {
        assertFlagged("LegacyBillingService.setRepository", "@Resource");
    }

    @Test
    void flagsAnAutowiredMethodWithoutTheSetPrefix() {
        assertFlagged("ReportScheduler.wire", "@Autowired");
    }

    @Test
    void reportsOneViolationPerAnnotatedMethod() {
        assertEquals(6, violationsIn(examples).size(), String.join("\n", violationsIn(examples)));
    }

    @Test
    void ignoresAPlainSetter() {
        assertNotReported("setLabel");
    }

    @Test
    void ignoresOtherAnnotationsFromTheResourcePackage() {
        assertNotReported("warmUp");
    }

    @Test
    void ignoresConstructorInjection() {
        assertNotReported("PaymentService");
    }

    @Test
    void ignoresABeanFactoryMethod() {
        assertNotReported("RepositoryConfiguration");
    }

    @Test
    void ignoresAnAutowiredSetterInTestOutput() {
        assertNotReported("TestOutputWiredHelper");
    }

    /** The same class file as above, moved: only its location changed, so only scope can explain the difference. */
    @Test
    void flagsTheTestOutputHelperOnceItSitsInProductionOutput() throws IOException {
        Path productionOutput = ProductionLayout.copy(outputRoot.resolve("helper"),
                List.of(TestOutputWiredHelper.class));
        JavaClasses helper = new ClassFileImporter().importPath(productionOutput);

        List<String> violations = violationsIn(helper);

        assertTrue(violations.stream().anyMatch(line -> line.contains("TestOutputWiredHelper.setRepository")),
                String.join("\n", violations));
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoSetterInjectionRule.rule, ID, examples);

        assertTrue(debt.contains("OrderService.setRepository"), debt);
        assertTrue(debt.contains("InvoiceService.setRepository"), debt);
        assertTrue(debt.contains("ShippingService.setRepository"), debt);
        assertTrue(debt.contains("LegacyAuditService.setRepository"), debt);
        assertTrue(debt.contains("LegacyBillingService.setRepository"), debt);
        assertTrue(debt.contains("ReportScheduler.wire"), debt);
    }
}
