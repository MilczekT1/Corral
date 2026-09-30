package io.github.milczekt1.corral.rules.spring.nofieldinjection;

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
import io.github.milczekt1.corral.rules.spring.nofieldinjection.fixtures.AuditTrail;
import io.github.milczekt1.corral.rules.spring.nofieldinjection.fixtures.BillingLedger;
import io.github.milczekt1.corral.rules.spring.nofieldinjection.fixtures.CheckoutService;
import io.github.milczekt1.corral.rules.spring.nofieldinjection.fixtures.MailSender;
import io.github.milczekt1.corral.rules.spring.nofieldinjection.fixtures.OrderRepository;
import io.github.milczekt1.corral.rules.spring.nofieldinjection.fixtures.PricingService;
import io.github.milczekt1.corral.rules.spring.nofieldinjection.fixtures.ReportWriter;
import io.github.milczekt1.corral.rules.spring.nofieldinjection.fixtures.TestOutputInjectedHelper;
import io.github.milczekt1.corral.rules.spring.nofieldinjection.fixtures.WiringConfiguration;
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
 * each example class carries exactly one of the matched annotations, and the flagged field names which.
 *
 * <p>The rule is scoped to production classes, and every fixture compiles into test output. So the
 * examples are copied into a Gradle main-output layout and imported from there;
 * {@link TestOutputInjectedHelper} alone is imported from where it compiled.
 */
class NoFieldInjectionRuleTest {

    private static final String ID = "corral.spring.no-field-injection";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    /** Matched by none of {@code TestScope}'s test-output patterns. */
    private static final String PRODUCTION_LAYOUT = "build/classes/java/main";

    private static final List<Class<?>> PRODUCTION_EXAMPLES = List.of(
            CheckoutService.class, MailSender.class, ReportWriter.class, AuditTrail.class,
            BillingLedger.class, PricingService.class, WiringConfiguration.class, OrderRepository.class);

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        Path productionOutput = copyIntoProductionLayout(outputRoot.resolve("examples"), PRODUCTION_EXAMPLES);
        examples = new ClassFileImporter().importLocations(List.of(
                Location.of(productionOutput),
                Location.of(classFileOf(TestOutputInjectedHelper.class))));

        assertEquals(PRODUCTION_EXAMPLES.size() + 1, examples.size(),
                "every example must be imported, or the ignore assertions below are vacuous");
    }

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violationsIn(JavaClasses classes) {
        return NoFieldInjectionRule.DEFINITION.evaluate(classes).getFailureReport().getDetails();
    }

    private static void assertFlagged(String field, String annotation) {
        List<String> violations = violationsIn(examples);

        assertTrue(violations.stream().anyMatch(line -> line.contains(field + ">") && line.contains(annotation)),
                String.join("\n", violations));
    }

    private static void assertNotReported(String subject) {
        String report = String.join("\n", violationsIn(examples));

        assertFalse(report.contains(subject), report);
    }

    @Test
    void flagsAnAutowiredField() {
        assertFlagged("CheckoutService.orders", "@Autowired");
    }

    @Test
    void flagsAJakartaInjectField() {
        assertFlagged("MailSender.outbox", "@Inject");
    }

    @Test
    void flagsAJakartaResourceField() {
        assertFlagged("ReportWriter.source", "@Resource");
    }

    @Test
    void flagsAJavaxInjectField() {
        assertFlagged("AuditTrail.entries", "@Inject");
    }

    @Test
    void flagsAJavaxResourceField() {
        assertFlagged("BillingLedger.invoices", "@Resource");
    }

    @Test
    void reportsEachInjectedFieldOfAClassSeparately() {
        assertFlagged("CheckoutService.archive", "@Autowired");
        assertEquals(6, violationsIn(examples).size(), String.join("\n", violationsIn(examples)));
    }

    @Test
    void ignoresAValueField() {
        assertNotReported("CheckoutService.prefix");
    }

    @Test
    void ignoresOtherAnnotationsFromTheResourcePackage() {
        assertNotReported("ReportWriter.footer");
    }

    @Test
    void ignoresUnannotatedAndStaticFields() {
        assertNotReported("CheckoutService.label");
        assertNotReported("CheckoutService.SEPARATOR");
    }

    @Test
    void ignoresConstructorInjection() {
        assertNotReported("PricingService");
    }

    @Test
    void ignoresAnAutowiredBeanMethodParameter() {
        assertNotReported("WiringConfiguration");
    }

    @Test
    void ignoresAnAutowiredFieldInTestOutput() {
        assertNotReported("TestOutputInjectedHelper");
    }

    /** The same class file as above, moved: only its location changed, so only scope can explain the difference. */
    @Test
    void flagsTheTestOutputHelperOnceItSitsInProductionOutput() throws IOException {
        Path productionOutput = copyIntoProductionLayout(outputRoot.resolve("helper"), List.of(TestOutputInjectedHelper.class));
        JavaClasses helper = new ClassFileImporter().importPath(productionOutput);

        List<String> violations = violationsIn(helper);

        assertTrue(violations.stream().anyMatch(line -> line.contains("TestOutputInjectedHelper.seeded")),
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoFieldInjectionRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(examples);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("CheckoutService.orders"), debt);
            assertTrue(debt.contains("CheckoutService.archive"), debt);
            assertTrue(debt.contains("MailSender.outbox"), debt);
            assertTrue(debt.contains("ReportWriter.source"), debt);
            assertTrue(debt.contains("AuditTrail.entries"), debt);
            assertTrue(debt.contains("BillingLedger.invoices"), debt);
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
