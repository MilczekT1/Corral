package io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures.TestOutputAbstractBean;
import io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures.production.AbstractBillingService;
import io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures.production.AbstractLedgerPolicy;
import io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures.production.AbstractLookupSupport;
import io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures.production.AbstractOrderHandler;
import io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures.production.AbstractPaymentHandler;
import io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures.production.AbstractPersistenceConfig;
import io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures.production.AbstractPrototypeFactory;
import io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures.production.AbstractRefundHandler;
import io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures.production.CheckoutHandler;
import io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures.production.DomainService;
import io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures.production.OrderRepository;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import io.github.milczekt1.corral.rules.testsupport.FixtureVerdicts;
import io.github.milczekt1.corral.rules.testsupport.ProductionLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NoComponentOnAbstractClassRuleTest {

    private static final String ID = "corral.spring.no-component-on-abstract-class";

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        examples = ProductionLayout.importFixtures(NoComponentOnAbstractClassRuleTest.class, outputRoot.resolve("examples"));
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
    void everyFixtureMatchesItsVerdict() {
        FixtureVerdicts.assertMatch(NoComponentOnAbstractClassRuleTest.class, NoComponentOnAbstractClassRule.DEFINITION, examples);
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
