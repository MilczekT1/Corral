package io.github.milczekt1.corral.rules.spring.nofinalcomponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.spring.nofinalcomponent.fixtures.CheckoutController;
import io.github.milczekt1.corral.rules.spring.nofinalcomponent.fixtures.CustomerRepository;
import io.github.milczekt1.corral.rules.spring.nofinalcomponent.fixtures.IndexedLookup;
import io.github.milczekt1.corral.rules.spring.nofinalcomponent.fixtures.Money;
import io.github.milczekt1.corral.rules.spring.nofinalcomponent.fixtures.OrderService;
import io.github.milczekt1.corral.rules.spring.nofinalcomponent.fixtures.PaymentConfig;
import io.github.milczekt1.corral.rules.spring.nofinalcomponent.fixtures.PaymentService;
import io.github.milczekt1.corral.rules.spring.nofinalcomponent.fixtures.PricingPolicy;
import io.github.milczekt1.corral.rules.spring.nofinalcomponent.fixtures.RefundHandler;
import io.github.milczekt1.corral.rules.spring.nofinalcomponent.fixtures.ShippingConfig;
import io.github.milczekt1.corral.rules.spring.nofinalcomponent.fixtures.TestOutputFinalBean;
import io.github.milczekt1.corral.rules.spring.nofinalcomponent.fixtures.UseCase;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import io.github.milczekt1.corral.rules.testsupport.ProductionLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The rule is scoped to production classes, and every fixture compiles into test output. So the
 * examples are copied into a Gradle main-output layout and imported from there;
 * {@link TestOutputFinalBean} alone is imported from where it compiled.
 */
class NoFinalComponentRuleTest {

    private static final String ID = "corral.spring.no-final-component";

    private static final List<Class<?>> PRODUCTION_EXAMPLES = List.of(
            OrderService.class, CustomerRepository.class, CheckoutController.class, PricingPolicy.class,
            PaymentConfig.class, ShippingConfig.class, UseCase.class, RefundHandler.class,
            PaymentService.class, Money.class, IndexedLookup.class);

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        Path productionOutput = ProductionLayout.copy(outputRoot.resolve("examples"), PRODUCTION_EXAMPLES);
        examples = new ClassFileImporter().importLocations(
                ProductionLayout.withTestOutput(productionOutput, TestOutputFinalBean.class));

        assertEquals(PRODUCTION_EXAMPLES.size() + 1, examples.size(),
                "every example must be imported, or the ignore assertions below are vacuous");
    }

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violationsIn(JavaClasses classes) {
        return NoFinalComponentRule.DEFINITION.evaluate(classes).getFailureReport().getDetails();
    }

    private static void assertFlagged(String type) {
        List<String> violations = violationsIn(examples);

        assertTrue(violations.stream().anyMatch(line -> line.contains("." + type + ">")),
                String.join("\n", violations));
    }

    private static void assertNotReported(String subject) {
        String report = String.join("\n", violationsIn(examples));

        assertFalse(report.contains(subject), report);
    }

    @Test
    void flagsAFinalService() {
        assertFlagged("OrderService");
    }

    @Test
    void flagsAFinalRepository() {
        assertFlagged("CustomerRepository");
    }

    @Test
    void flagsAFinalController() {
        assertFlagged("CheckoutController");
    }

    @Test
    void flagsAComponentRecord() {
        assertFlagged("PricingPolicy");
    }

    @Test
    void flagsAFinalConfiguration() {
        assertFlagged("ShippingConfig");
    }

    @Test
    void flagsAFinalConfigurationWithoutBeanMethodProxying() {
        assertFlagged("PaymentConfig");
    }

    @Test
    void flagsAFinalClassCarryingAComposedStereotype() {
        assertFlagged("RefundHandler");
    }

    @Test
    void reportsOneViolationPerFinalBean() {
        assertEquals(7, violationsIn(examples).size(), String.join("\n", violationsIn(examples)));
    }

    @Test
    void ignoresANonFinalBean() {
        assertNotReported("PaymentService");
    }

    @Test
    void ignoresAFinalClassThatIsNotABean() {
        assertNotReported("Money");
    }

    @Test
    void ignoresAFinalClassCarryingANonStereotypeAnnotationFromTheStereotypePackage() {
        assertNotReported("IndexedLookup");
    }

    @Test
    void ignoresTheComposedStereotypeAnnotationItself() {
        assertNotReported("UseCase");
    }

    @Test
    void ignoresAFinalBeanInTestOutput() {
        assertNotReported("TestOutputFinalBean");
    }

    /** The same class file as above, moved: only its location changed, so only scope can explain the difference. */
    @Test
    void flagsTheTestOutputBeanOnceItSitsInProductionOutput() throws IOException {
        Path productionOutput = ProductionLayout.copy(outputRoot.resolve("bean"), List.of(TestOutputFinalBean.class));
        JavaClasses bean = new ClassFileImporter().importPath(productionOutput);

        List<String> violations = violationsIn(bean);

        assertTrue(violations.stream().anyMatch(line -> line.contains("TestOutputFinalBean")),
                String.join("\n", violations));
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoFinalComponentRule.rule, ID, examples);

        assertTrue(debt.contains("OrderService>"), debt);
        assertTrue(debt.contains("CustomerRepository>"), debt);
        assertTrue(debt.contains("CheckoutController>"), debt);
        assertTrue(debt.contains("PricingPolicy>"), debt);
        assertTrue(debt.contains("PaymentConfig>"), debt);
        assertTrue(debt.contains("ShippingConfig>"), debt);
        assertTrue(debt.contains("RefundHandler>"), debt);
    }
}
