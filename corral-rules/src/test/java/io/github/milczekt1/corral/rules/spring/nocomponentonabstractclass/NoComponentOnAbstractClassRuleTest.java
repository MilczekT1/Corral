package io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass;

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
 * {@link TestOutputAbstractBean} alone is imported from where it compiled.
 */
class NoComponentOnAbstractClassRuleTest {

    private static final String ID = "corral.spring.no-component-on-abstract-class";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    /** Matched by none of {@code TestScope}'s test-output patterns. */
    private static final String PRODUCTION_LAYOUT = "build/classes/java/main";

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
        Path productionOutput = copyIntoProductionLayout(outputRoot.resolve("examples"), PRODUCTION_EXAMPLES);
        examples = new ClassFileImporter().importLocations(List.of(
                Location.of(productionOutput),
                Location.of(classFileOf(TestOutputAbstractBean.class))));

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
        Path productionOutput = copyIntoProductionLayout(outputRoot.resolve("bean"), List.of(TestOutputAbstractBean.class));
        JavaClasses bean = new ClassFileImporter().importPath(productionOutput);

        List<String> violations = violationsIn(bean);

        assertTrue(violations.stream().anyMatch(line -> line.contains("TestOutputAbstractBean")),
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoComponentOnAbstractClassRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(examples);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("AbstractOrderHandler>"), debt);
            assertTrue(debt.contains("AbstractBillingService>"), debt);
            assertTrue(debt.contains("AbstractLedgerPolicy>"), debt);
            assertTrue(debt.contains("AbstractRefundHandler>"), debt);
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
