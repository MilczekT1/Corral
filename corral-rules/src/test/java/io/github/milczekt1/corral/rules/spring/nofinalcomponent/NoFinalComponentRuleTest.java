package io.github.milczekt1.corral.rules.spring.nofinalcomponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
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
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
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

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    /** Matched by none of {@code TestScope}'s test-output patterns. */
    private static final String PRODUCTION_LAYOUT = "build/classes/java/main";

    private static final List<Class<?>> PRODUCTION_EXAMPLES = List.of(
            OrderService.class, CustomerRepository.class, CheckoutController.class, PricingPolicy.class,
            PaymentConfig.class, ShippingConfig.class, UseCase.class, RefundHandler.class,
            PaymentService.class, Money.class, IndexedLookup.class);

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        Path productionOutput = copyIntoProductionLayout(outputRoot.resolve("examples"), PRODUCTION_EXAMPLES);
        examples = new ClassFileImporter().importLocations(List.of(
                Location.of(productionOutput),
                Location.of(classFileOf(TestOutputFinalBean.class))));

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
        Path productionOutput = copyIntoProductionLayout(outputRoot.resolve("bean"), List.of(TestOutputFinalBean.class));
        JavaClasses bean = new ClassFileImporter().importPath(productionOutput);

        List<String> violations = violationsIn(bean);

        assertTrue(violations.stream().anyMatch(line -> line.contains("TestOutputFinalBean")),
                String.join("\n", violations));
    }

    /**
     * {@link FreezingArchRule#persistIn}, not {@code freeze.store}: a frozen rule captures its store
     * when constructed, which is class initialisation, so naming one here races class loading. Only
     * the path goes on the process-wide {@link ArchConfiguration}.
     *
     * <p>Reseed with {@code -Darchunit.freeze.store.default.allowStoreCreation=true}, then commit.
     */
    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        ArchConfiguration.get().setProperty("freeze.store.default.path", STORE_PATH);
        try {
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoFinalComponentRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(examples);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("OrderService>"), debt);
            assertTrue(debt.contains("CustomerRepository>"), debt);
            assertTrue(debt.contains("CheckoutController>"), debt);
            assertTrue(debt.contains("PricingPolicy>"), debt);
            assertTrue(debt.contains("PaymentConfig>"), debt);
            assertTrue(debt.contains("ShippingConfig>"), debt);
            assertTrue(debt.contains("RefundHandler>"), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }

    /** Copies each class file into {@code root/}{@value #PRODUCTION_LAYOUT}, keeping package directories. */
    private static Path copyIntoProductionLayout(Path root, List<Class<?>> classes) throws IOException {
        Path output = root.resolve(PRODUCTION_LAYOUT);
        for (Class<?> type : classes) {
            Path destination = output.resolve(type.getName().replace('.', '/') + ".class");
            Files.createDirectories(destination.getParent());
            try (InputStream compiled = classFileOf(type).openStream()) {
                Files.copy(compiled, destination);
            }
        }
        return output;
    }

    private static URL classFileOf(Class<?> type) {
        URL classFile = type.getResource(type.getSimpleName() + ".class");
        assertNotNull(classFile, () -> "no compiled class file on the classpath for " + type.getName());
        return classFile;
    }
}
