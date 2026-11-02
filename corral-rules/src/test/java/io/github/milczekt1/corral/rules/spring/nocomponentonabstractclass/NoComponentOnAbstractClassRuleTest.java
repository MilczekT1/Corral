package io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures.AbstractBillingService;
import io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures.AbstractLedgerPolicy;
import io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures.AbstractLookupSupport;
import io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures.AbstractOrderHandler;
import io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures.AbstractPaymentHandler;
import io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures.AbstractPersistenceConfig;
import io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures.AbstractPrototypeFactory;
import io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures.AbstractRefundHandler;
import io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures.CheckoutHandler;
import io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures.DomainService;
import io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures.OrderRepository;
import io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures.TestOutputAbstractBean;
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
 * {@link TestOutputAbstractBean} alone is imported from where it compiled.
 */
class NoComponentOnAbstractClassRuleTest {

    private static final String ID = "corral.spring.no-component-on-abstract-class";

    private static final List<Class<?>> PRODUCTION_EXAMPLES = List.of(
            AbstractOrderHandler.class, AbstractBillingService.class, AbstractLedgerPolicy.class,
            AbstractRefundHandler.class, AbstractLookupSupport.class, AbstractPrototypeFactory.class,
            AbstractPersistenceConfig.class, AbstractPaymentHandler.class, CheckoutHandler.class,
            OrderRepository.class, DomainService.class);

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        Path productionOutput = ProductionLayout.copy(outputRoot.resolve("examples"), PRODUCTION_EXAMPLES);
        examples = new ClassFileImporter().importLocations(
                ProductionLayout.withTestOutput(productionOutput, TestOutputAbstractBean.class));

        assertEquals(PRODUCTION_EXAMPLES.size() + 1, examples.size(),
                "every example must be imported, or the ignore assertions below are vacuous");
    }

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violationsIn(JavaClasses classes) {
        return NoComponentOnAbstractClassRule.DEFINITION.evaluate(classes).getFailureReport().getDetails();
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
    void flagsAnAbstractComponent() {
        assertFlagged("AbstractOrderHandler");
    }

    @Test
    void flagsAnAbstractService() {
        assertFlagged("AbstractBillingService");
    }

    @Test
    void flagsAnAbstractClassCarryingAComposedStereotype() {
        assertFlagged("AbstractLedgerPolicy");
    }

    @Test
    void flagsAnAbstractComponentThatOnlyInheritsALookupMethod() {
        assertFlagged("AbstractRefundHandler");
    }

    @Test
    void reportsOneViolationPerAbstractStereotypedClass() {
        assertEquals(4, violationsIn(examples).size(), String.join("\n", violationsIn(examples)));
    }

    @Test
    void ignoresAConcreteBeanExtendingAFlaggedBase() {
        assertNotReported("CheckoutHandler");
    }

    @Test
    void ignoresAnAbstractClassWithoutAStereotype() {
        assertNotReported("AbstractPaymentHandler");
    }

    @Test
    void ignoresAnAbstractClassWithALookupMethodButNoStereotype() {
        assertNotReported("AbstractLookupSupport");
    }

    @Test
    void ignoresAnAbstractComponentDeclaringALookupMethod() {
        assertNotReported("AbstractPrototypeFactory");
    }

    @Test
    void ignoresAnAbstractConfiguration() {
        assertNotReported("AbstractPersistenceConfig");
    }

    @Test
    void ignoresAStereotypedInterface() {
        assertNotReported("OrderRepository");
    }

    @Test
    void ignoresTheComposedStereotypeAnnotationItself() {
        assertNotReported("DomainService");
    }

    @Test
    void ignoresAnAbstractComponentInTestOutput() {
        assertNotReported("TestOutputAbstractBean");
    }

    /** The same class file as above, moved: only its location changed, so only scope can explain the difference. */
    @Test
    void flagsTheTestOutputBeanOnceItSitsInProductionOutput() throws IOException {
        Path productionOutput = ProductionLayout.copy(outputRoot.resolve("bean"),
                List.of(TestOutputAbstractBean.class));
        JavaClasses bean = new ClassFileImporter().importPath(productionOutput);

        List<String> violations = violationsIn(bean);

        assertTrue(violations.stream().anyMatch(line -> line.contains("TestOutputAbstractBean")),
                String.join("\n", violations));
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoComponentOnAbstractClassRule.rule, ID, examples);

        assertTrue(debt.contains("AbstractOrderHandler>"), debt);
        assertTrue(debt.contains("AbstractBillingService>"), debt);
        assertTrue(debt.contains("AbstractLedgerPolicy>"), debt);
        assertTrue(debt.contains("AbstractRefundHandler>"), debt);
    }
}
