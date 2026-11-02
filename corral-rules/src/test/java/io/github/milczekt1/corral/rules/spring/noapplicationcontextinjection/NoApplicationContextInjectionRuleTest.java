package io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures.AuditJob;
import io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures.Basket;
import io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures.CatalogService;
import io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures.ContextAwareRegistry;
import io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures.ContextWiringTest;
import io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures.FactoryAwareRegistry;
import io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures.HolderClient;
import io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures.InvoiceService;
import io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures.LookupSupport;
import io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures.OrderService;
import io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures.PaymentService;
import io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures.PricingStrategy;
import io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures.ReportJob;
import io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures.ShippingService;
import io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures.ShopApplication;
import io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures.SpringContextHolder;
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
 * examples are copied into a Gradle main-output layout and imported from there.
 */
class NoApplicationContextInjectionRuleTest {

    private static final String ID = "corral.spring.no-application-context-injection";

    private static final List<Class<?>> EXAMPLES = List.of(
            OrderService.class, InvoiceService.class, CatalogService.class, ContextAwareRegistry.class,
            FactoryAwareRegistry.class, SpringContextHolder.class, HolderClient.class, LookupSupport.class,
            ReportJob.class, AuditJob.class, PaymentService.class, ShippingService.class,
            ShopApplication.class, ContextWiringTest.class, PricingStrategy.class, Basket.class);

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        examples = new ClassFileImporter().importPath(ProductionLayout.copy(outputRoot, EXAMPLES));

        assertEquals(EXAMPLES.size(), examples.size(),
                "every example must be imported, or the ignore assertions below are vacuous");
    }

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violations() {
        return NoApplicationContextInjectionRule.DEFINITION.evaluate(examples).getFailureReport().getDetails();
    }

    /** A violation reads {@code <origin> <verb> <target>}; the origin ends at the first {@code "> "}. */
    private static String originOf(String violation) {
        return violation.substring(0, violation.indexOf("> ") + 1);
    }

    private static String targetOf(String violation) {
        return violation.substring(violation.indexOf("> ") + 1);
    }

    private static boolean names(String description, String type) {
        return description.contains("." + type + ">") || description.contains("." + type + ".");
    }

    private static void assertFlagged(String origin, String container) {
        List<String> violations = violations();

        assertTrue(violations.stream().anyMatch(line ->
                        names(originOf(line), origin) && names(targetOf(line), container)),
                String.join("\n", violations));
    }

    private static void assertCallbackFlagged(String callback, String container) {
        List<String> violations = violations();

        assertTrue(violations.stream().anyMatch(line -> line.contains("." + callback)
                        && line.contains(" has parameter of type <") && names(targetOf(line), container)),
                String.join("\n", violations));
    }

    private static void assertNotReported(String origin) {
        List<String> violations = violations();

        assertFalse(violations.stream().anyMatch(line -> names(originOf(line), origin)),
                String.join("\n", violations));
    }

    private static void assertNoDependencyReportedOn(String type) {
        List<String> violations = violations();

        assertFalse(violations.stream().anyMatch(line -> names(targetOf(line), type)),
                String.join("\n", violations));
    }

    @Test
    void flagsAnInjectedApplicationContext() {
        assertFlagged("OrderService", "ApplicationContext");
    }

    @Test
    void flagsAnInjectedConfigurableApplicationContext() {
        assertFlagged("InvoiceService", "ConfigurableApplicationContext");
    }

    @Test
    void flagsAnInjectedListableBeanFactory() {
        assertFlagged("CatalogService", "ListableBeanFactory");
    }

    @Test
    void flagsTheCallbackParameterOfAnApplicationContextAwareImplementation() {
        assertCallbackFlagged("ContextAwareRegistry.setApplicationContext(", "ApplicationContext");
    }

    @Test
    void flagsTheCallbackParameterOfABeanFactoryAwareImplementation() {
        assertCallbackFlagged("FactoryAwareRegistry.setBeanFactory(", "BeanFactory");
    }

    @Test
    void flagsAStaticContextHolder() {
        assertFlagged("SpringContextHolder", "ApplicationContext");
    }

    @Test
    void flagsAnAwareBaseClassAndASubclassCallingItsContext() {
        assertFlagged("LookupSupport", "ApplicationContext");
        assertFlagged("ReportJob", "ApplicationContext");
    }

    @Test
    void reportsEachDependencyOnTheContainer() {
        List<String> violations = violations();

        assertEquals(23, violations.size(), String.join("\n", violations));
    }

    @Test
    void ignoresTheStrategyLookedUpThroughTheContainer() {
        assertNoDependencyReportedOn("PricingStrategy");
        assertNoDependencyReportedOn("Basket");
    }

    @Test
    void ignoresACallerOfAStaticContextHolder() {
        assertNotReported("HolderClient");
    }

    @Test
    void ignoresASubclassCallingALookupHelperOfItsBaseClass() {
        assertNotReported("AuditJob");
    }

    @Test
    void ignoresAMapOfStrategiesAndAnEventPublisher() {
        assertNotReported("PaymentService");
    }

    @Test
    void ignoresAnObjectProvider() {
        assertNotReported("ShippingService");
    }

    @Test
    void ignoresABootMainThatOnlyRunsTheApplication() {
        assertNotReported("ShopApplication");
    }

    @Test
    void ignoresATestClassAutowiringTheContext() {
        assertNotReported("ContextWiringTest");
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoApplicationContextInjectionRule.rule, ID, examples);

        assertTrue(debt.contains(".OrderService.context> has type <"), debt);
        assertTrue(debt.contains(".InvoiceService.context> has type <"), debt);
        assertTrue(debt.contains(".CatalogService.beans> has type <"), debt);
        assertTrue(debt.contains(".ContextAwareRegistry.context> has type <"), debt);
        assertTrue(debt.contains(".FactoryAwareRegistry.beanFactory> has type <"), debt);
        assertTrue(debt.contains(".ReportJob.run("), debt);
    }
}
