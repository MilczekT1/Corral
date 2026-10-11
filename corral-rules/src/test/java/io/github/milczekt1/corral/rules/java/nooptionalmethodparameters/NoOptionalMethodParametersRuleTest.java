package io.github.milczekt1.corral.rules.java.nooptionalmethodparameters;

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
import io.github.milczekt1.corral.rules.java.nooptionalmethodparameters.fixtures.OrderQuery;
import io.github.milczekt1.corral.rules.java.nooptionalmethodparameters.fixtures.TestOutputLookup;
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
 * fixture class files — anonymous ones included — are copied into a Gradle main-output layout and
 * imported from there; {@link TestOutputLookup} alone is imported from where it compiled.
 */
class NoOptionalMethodParametersRuleTest {

    private static final String ID = "corral.java.no-optional-method-parameters";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    /** Matched by none of {@code TestScope}'s test-output patterns. */
    private static final String PRODUCTION_LAYOUT = "build/classes/java/main";

    private static final String RAW_TYPES =
            "has raw parameter types containing Optional, OptionalInt, OptionalLong or OptionalDouble";

    /** Thirteen fixture sources, plus {@code ReminderTask$1}. */
    private static final int FIXTURE_CLASS_FILES = 14;

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        Path productionOutput = copyIntoProductionLayout(outputRoot.resolve("examples"),
                name -> !name.startsWith(TestOutputLookup.class.getSimpleName()));
        examples = new ClassFileImporter().importLocations(List.of(
                Location.of(productionOutput),
                Location.of(classFileOf(TestOutputLookup.class))));

        assertEquals(FIXTURE_CLASS_FILES, examples.size(),
                "every example must be imported, or the ignore assertions below are vacuous");
    }

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violationsIn(JavaClasses classes) {
        return NoOptionalMethodParametersRule.DEFINITION.evaluate(classes).getFailureReport().getDetails();
    }

    private static List<String> violations() {
        return violationsIn(examples);
    }

    private static void assertFlagged(String method) {
        List<String> violations = violations();

        assertTrue(violations.stream().anyMatch(line -> line.contains("." + method + "(") && line.contains(RAW_TYPES)),
                String.join("\n", violations));
    }

    private static void assertNotReported(String subject) {
        String report = String.join("\n", violations());

        assertFalse(report.contains("." + subject), report);
    }

    @Test
    void flagsAnOptionalParameter() {
        assertFlagged("OrderQuery.find");
    }

    @Test
    void flagsAnOptionalIntParameter() {
        assertFlagged("Thresholds.withRetries");
    }

    @Test
    void flagsAnOptionalLongParameter() {
        assertFlagged("Thresholds.withTimeout");
    }

    @Test
    void flagsAnOptionalDoubleParameter() {
        assertFlagged("Thresholds.withRatio");
    }

    @Test
    void flagsAnEnumMethod() {
        assertFlagged("ShipmentStatus.describe");
    }

    @Test
    void flagsAComparatorOfOptionalOnlyOnItsDeclaredMethod() {
        List<String> violations = violations();

        assertFlagged("PresenceComparator.compare");
        assertEquals(1, violations.stream().filter(line -> line.contains(".PresenceComparator.")).count(),
                String.join("\n", violations));
    }

    @Test
    void flagsACovariantOverrideOnceNotItsBridge() {
        List<String> violations = violations();

        assertFlagged("ArchiveQuery.find");
        assertEquals(1, violations.stream().filter(line -> line.contains(".ArchiveQuery.")).count(),
                String.join("\n", violations));
    }

    @Test
    void flagsAStaticMethod() {
        assertFlagged("RegionCodes.parse");
    }

    @Test
    void flagsAPrivateMethod() {
        assertFlagged("RegionCodes.fallback");
    }

    @Test
    void flagsAnInterfaceMethod() {
        assertFlagged("CustomerLookup.lookup");
    }

    @Test
    void reportsOneViolationPerMethod() {
        List<String> violations = violations();

        assertEquals(10, violations.size(), String.join("\n", violations));
    }

    @Test
    void ignoresThePlainParameterBesideAFlaggedOne() {
        assertNotReported("OrderQuery.findByStatus(");
    }

    @Test
    void ignoresAnOptionalReturnType() {
        assertNotReported("OrderQuery.latest(");
    }

    @Test
    void ignoresOverloadsAndAnOptionalConstructorParameter() {
        assertNotReported("PaymentQuery");
    }

    @Test
    void ignoresARecordWithAnOptionalComponent() {
        assertNotReported("RefundView");
    }

    @Test
    void ignoresAnOptionalTypeArgumentAndOptionalVarargs() {
        assertNotReported("AuditTrail");
    }

    @Test
    void ignoresALambdaOverOptionalsAndAnAnonymousClassCapturingOne() {
        assertNotReported("ReminderTask");
    }

    @Test
    void ignoresAParameterizedTestMethod() {
        assertNotReported("DiscountCases");
    }

    @Test
    void ignoresAMethodInTestOutput() {
        assertNotReported("TestOutputLookup");
    }

    /** The same class file as above, moved: only its location changed, so only scope can explain the difference. */
    @Test
    void flagsTheTestOutputMethodOnceItSitsInProductionOutput() throws IOException {
        Path productionOutput = copyIntoProductionLayout(outputRoot.resolve("lookup"),
                name -> name.equals(TestOutputLookup.class.getSimpleName() + ".class"));

        List<String> violations = violationsIn(new ClassFileImporter().importPath(productionOutput));

        assertTrue(violations.stream().anyMatch(line -> line.contains(".TestOutputLookup.apply(")),
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoOptionalMethodParametersRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(examples);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains(".OrderQuery.find(java.util.Optional)> " + RAW_TYPES), debt);
            assertTrue(debt.contains(".Thresholds.withRetries(java.util.OptionalInt)> " + RAW_TYPES), debt);
            assertTrue(debt.contains(".PresenceComparator.compare(java.util.Optional, java.util.Optional)> " + RAW_TYPES),
                    debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }

    /**
     * Copies the fixture class files whose names pass {@code include} into
     * {@code root/}{@value #PRODUCTION_LAYOUT}, keeping package directories.
     */
    private static Path copyIntoProductionLayout(Path root, Predicate<String> include) throws IOException {
        Path destination = root.resolve(PRODUCTION_LAYOUT)
                .resolve(OrderQuery.class.getPackageName().replace('.', '/'));
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
            return Path.of(classFileOf(OrderQuery.class).toURI()).getParent();
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
