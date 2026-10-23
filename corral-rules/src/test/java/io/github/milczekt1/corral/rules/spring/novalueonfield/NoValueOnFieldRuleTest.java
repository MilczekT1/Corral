package io.github.milczekt1.corral.rules.spring.novalueonfield;

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
import io.github.milczekt1.corral.rules.spring.novalueonfield.fixtures.OrderService;
import io.github.milczekt1.corral.rules.spring.novalueonfield.fixtures.TestOutputPortHolder;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The rule is scoped to production classes, and every fixture compiles into test output. So the
 * fixture class files are copied into a Gradle main-output layout and imported from there; the two
 * JUnit test classes among them are then test classes only by their declared test methods.
 * {@link TestOutputPortHolder} alone is imported from where it compiled.
 */
class NoValueOnFieldRuleTest {

    private static final String ID = "corral.spring.no-value-on-field";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    /** Matched by none of {@code TestScope}'s test-output patterns. */
    private static final String PRODUCTION_LAYOUT = "build/classes/java/main";

    private static final String META_ANNOTATED = "> is meta-annotated with @Value";

    private static final int FIXTURE_CLASS_FILES = 12;

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        Path productionOutput = copyIntoProductionLayout(outputRoot.resolve("examples"),
                name -> !name.startsWith(TestOutputPortHolder.class.getSimpleName()));
        examples = new ClassFileImporter().importLocations(List.of(
                Location.of(productionOutput),
                Location.of(classFileOf(TestOutputPortHolder.class))));

        assertEquals(FIXTURE_CLASS_FILES, examples.size(),
                "every example must be imported, or the ignore assertions below are vacuous");
    }

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violationsIn(JavaClasses classes) {
        return NoValueOnFieldRule.DEFINITION.evaluate(classes).getFailureReport().getDetails();
    }

    private static void assertFlagged(String field) {
        List<String> violations = violationsIn(examples);

        assertTrue(violations.stream().anyMatch(line -> line.contains("." + field + META_ANNOTATED)),
                String.join("\n", violations));
    }

    private static void assertNotReported(String subject) {
        String report = String.join("\n", violationsIn(examples));

        assertFalse(report.contains("." + subject), report);
    }

    @Test
    void flagsAValueField() {
        assertFlagged("OrderService.timeout");
    }

    @Test
    void flagsAValueFieldWithASpelExpression() {
        assertFlagged("ReminderScheduler.interval");
    }

    @Test
    void flagsAStaticValueField() {
        assertFlagged("CacheSettings.region");
    }

    @Test
    void flagsAStaticFinalValueFieldWithAnInitialiser() {
        assertFlagged("CacheSettings.MAX_ENTRIES");
    }

    @Test
    void flagsAFieldAnnotatedWithAnAnnotationCarryingValue() {
        assertFlagged("ShippingService.timeout");
    }

    @Test
    void reportsOneViolationPerValueField() {
        List<String> violations = violationsIn(examples);

        assertEquals(6, violations.size(), String.join("\n", violations));
    }

    @Test
    void ignoresAnUnannotatedFieldBesideAFlaggedOne() {
        assertNotReported("OrderService.grace");
    }

    @Test
    void ignoresConstructorInjection() {
        assertNotReported("PaymentService");
    }

    @Test
    void ignoresARecordComponent() {
        assertNotReported("RefundSettings");
    }

    @Test
    void ignoresAFinalFieldAnnotatedBesideItsConstructorParameter() {
        assertNotReported("BillingService");
    }

    @Test
    void ignoresAValueFieldOfATestClass() {
        assertNotReported("LocalPortTest");
    }

    @Test
    void ignoresAStaticValueFieldOfATestClass() {
        assertNotReported("StaticPortTest");
    }

    @Test
    void flagsAProductionClassNamedLikeATest() {
        assertFlagged("PortSmokeTest.port");
    }

    @Test
    void ignoresAValueFieldOfAClassInTestOutput() {
        assertNotReported("TestOutputPortHolder");
    }

    /** The same class file as above, moved: only its location changed, so only scope can explain the difference. */
    @Test
    void flagsTheTestOutputHolderOnceItSitsInProductionOutput() throws IOException {
        Path productionOutput = copyIntoProductionLayout(outputRoot.resolve("holder"),
                name -> name.equals(TestOutputPortHolder.class.getSimpleName() + ".class"));
        JavaClasses holder = new ClassFileImporter().importPath(productionOutput);

        List<String> violations = violationsIn(holder);

        assertTrue(violations.stream().anyMatch(line -> line.contains(".TestOutputPortHolder.port" + META_ANNOTATED)),
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoValueOnFieldRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(examples);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains(".OrderService.timeout" + META_ANNOTATED), debt);
            assertTrue(debt.contains(".CacheSettings.MAX_ENTRIES" + META_ANNOTATED), debt);
            assertTrue(debt.contains(".ShippingService.timeout" + META_ANNOTATED), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }

    private static Path copyIntoProductionLayout(Path root, Predicate<String> include) throws IOException {
        Path destination = root.resolve(PRODUCTION_LAYOUT)
                .resolve(OrderService.class.getPackageName().replace('.', '/'));
        Files.createDirectories(destination);
        try (Stream<Path> classFiles = Files.list(fixturesDirectory())) {
            for (Path classFile : classFiles.filter(file -> include.test(file.getFileName().toString())).toList()) {
                Files.copy(classFile, destination.resolve(classFile.getFileName()));
            }
        }
        return root.resolve(PRODUCTION_LAYOUT);
    }

    private static Path fixturesDirectory() {
        try {
            return Path.of(classFileOf(OrderService.class).toURI()).getParent();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static URL classFileOf(Class<?> type) {
        URL classFile = type.getResource(type.getSimpleName() + ".class");
        assertNotNull(classFile, () -> "no compiled class file on the classpath for " + type.getName());
        return classFile;
    }
}
