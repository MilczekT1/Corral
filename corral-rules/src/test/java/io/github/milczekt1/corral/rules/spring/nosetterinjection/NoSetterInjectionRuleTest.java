package io.github.milczekt1.corral.rules.spring.nosetterinjection;

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
import io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures.InvoiceService;
import io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures.LegacyAuditService;
import io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures.LegacyBillingService;
import io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures.OrderRepository;
import io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures.OrderService;
import io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures.PaymentService;
import io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures.ReportScheduler;
import io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures.RepositoryConfiguration;
import io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures.ShippingService;
import io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures.TestOutputWiredHelper;
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
 * ArchUnit reports an annotation by simple name, so {@code jakarta} and {@code javax} read the same:
 * each example carries exactly one of the matched annotations, and the flagged method names which.
 *
 * <p>The rule is scoped to production classes, and every fixture compiles into test output. So the
 * examples are copied into a Gradle main-output layout and imported from there;
 * {@link TestOutputWiredHelper} alone is imported from where it compiled.
 */
class NoSetterInjectionRuleTest {

    private static final String ID = "corral.spring.no-setter-injection";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    /** Matched by none of {@code TestScope}'s test-output patterns. */
    private static final String PRODUCTION_LAYOUT = "build/classes/java/main";

    private static final List<Class<?>> PRODUCTION_EXAMPLES = List.of(
            OrderService.class, InvoiceService.class, ShippingService.class,
            LegacyAuditService.class, LegacyBillingService.class, ReportScheduler.class,
            PaymentService.class, RepositoryConfiguration.class, OrderRepository.class);

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        Path productionOutput = copyIntoProductionLayout(outputRoot.resolve("examples"), PRODUCTION_EXAMPLES);
        examples = new ClassFileImporter().importLocations(List.of(
                Location.of(productionOutput),
                Location.of(classFileOf(TestOutputWiredHelper.class))));

        assertEquals(PRODUCTION_EXAMPLES.size() + 1, examples.size(),
                "every example must be imported, or the ignore assertions below are vacuous");
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
        Path productionOutput = copyIntoProductionLayout(outputRoot.resolve("helper"), List.of(TestOutputWiredHelper.class));
        JavaClasses helper = new ClassFileImporter().importPath(productionOutput);

        List<String> violations = violationsIn(helper);

        assertTrue(violations.stream().anyMatch(line -> line.contains("TestOutputWiredHelper.setRepository")),
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoSetterInjectionRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(examples);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("OrderService.setRepository"), debt);
            assertTrue(debt.contains("InvoiceService.setRepository"), debt);
            assertTrue(debt.contains("ShippingService.setRepository"), debt);
            assertTrue(debt.contains("LegacyAuditService.setRepository"), debt);
            assertTrue(debt.contains("LegacyBillingService.setRepository"), debt);
            assertTrue(debt.contains("ReportScheduler.wire"), debt);
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
