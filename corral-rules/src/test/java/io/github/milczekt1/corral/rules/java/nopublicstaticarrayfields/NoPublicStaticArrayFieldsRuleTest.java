package io.github.milczekt1.corral.rules.java.nopublicstaticarrayfields;

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
import io.github.milczekt1.corral.rules.java.nopublicstaticarrayfields.fixtures.RegionCodes;
import io.github.milczekt1.corral.rules.java.nopublicstaticarrayfields.fixtures.TestOutputArrays;
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
 * fixture class files are copied into a Gradle main-output layout and imported from there;
 * {@link TestOutputArrays} alone is imported from where it compiled.
 */
class NoPublicStaticArrayFieldsRuleTest {

    private static final String ID = "corral.java.no-public-static-array-fields";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    /** Matched by none of {@code TestScope}'s test-output patterns. */
    private static final String PRODUCTION_LAYOUT = "build/classes/java/main";

    private static final String ARRAY = "> has raw type an array in (";

    private static final int FIXTURE_CLASS_FILES = 5;

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        Path productionOutput = copyIntoProductionLayout(outputRoot.resolve("examples"),
                name -> !name.startsWith(TestOutputArrays.class.getSimpleName()));
        examples = new ClassFileImporter().importLocations(List.of(
                Location.of(productionOutput),
                Location.of(classFileOf(TestOutputArrays.class))));

        assertEquals(FIXTURE_CLASS_FILES, examples.size(),
                "every example must be imported, or the ignore assertions below are vacuous");
    }

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violationsIn(JavaClasses classes) {
        return NoPublicStaticArrayFieldsRule.DEFINITION.evaluate(classes).getFailureReport().getDetails();
    }

    private static void assertFlagged(String field) {
        List<String> violations = violationsIn(examples);

        assertTrue(violations.stream().anyMatch(line -> line.contains("." + field + ARRAY)),
                String.join("\n", violations));
    }

    private static void assertNotReported(String subject) {
        String report = String.join("\n", violationsIn(examples));

        assertFalse(report.contains("." + subject), report);
    }

    @Test
    void flagsAFinalArrayConstant() {
        assertFlagged("RegionCodes.ISO_CODES");
    }

    @Test
    void flagsAZeroLengthArrayConstant() {
        assertFlagged("RegionCodes.EMPTY");
    }

    @Test
    void flagsANonFinalArray() {
        assertFlagged("RetryCounters.attempts");
    }

    @Test
    void flagsAnArrayConstantOfAnInterface() {
        assertFlagged("CurrencySymbols.SYMBOLS");
    }

    @Test
    void reportsOneViolationPerField() {
        List<String> violations = violationsIn(examples);

        assertEquals(4, violations.size(), String.join("\n", violations));
    }

    @Test
    void ignoresAnImmutableListConstant() {
        assertNotReported("RegionCodes.REGIONS");
    }

    @Test
    void ignoresAPrivateArray() {
        assertNotReported("RegionCodes.INTERNAL_PREFIXES");
    }

    @Test
    void ignoresAProtectedArray() {
        assertNotReported("RegionCodes.LEGACY_ALIASES");
    }

    @Test
    void ignoresAPackagePrivateArray() {
        assertNotReported("RegionCodes.PACKAGE_DEFAULTS");
    }

    @Test
    void ignoresAPublicInstanceArray() {
        assertNotReported("RegionCodes.localOverrides");
    }

    @Test
    void ignoresANonFinalNonArrayField() {
        assertNotReported("RetryCounters.enabled");
    }

    @Test
    void ignoresANonArrayInterfaceConstant() {
        assertNotReported("CurrencySymbols.DEFAULT_CURRENCY");
    }

    @Test
    void ignoresEnumConstantsAndTheirValuesArray() {
        assertNotReported("Priority");
    }

    @Test
    void ignoresAnArrayInTestOutput() {
        assertNotReported("TestOutputArrays");
    }

    @Test
    void flagsTheTestOutputArrayOnceItSitsInProductionOutput() throws IOException {
        Path productionOutput = copyIntoProductionLayout(outputRoot.resolve("arrays"),
                name -> name.equals(TestOutputArrays.class.getSimpleName() + ".class"));
        JavaClasses arrays = new ClassFileImporter().importPath(productionOutput);

        List<String> violations = violationsIn(arrays);

        assertTrue(violations.stream().anyMatch(line -> line.contains(".TestOutputArrays.FIXTURE_IDS" + ARRAY)),
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoPublicStaticArrayFieldsRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(examples);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains(".RegionCodes.ISO_CODES" + ARRAY), debt);
            assertTrue(debt.contains(".RegionCodes.EMPTY" + ARRAY), debt);
            assertTrue(debt.contains(".RetryCounters.attempts" + ARRAY), debt);
            assertTrue(debt.contains(".CurrencySymbols.SYMBOLS" + ARRAY), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }

    /**
     * Copies the fixture class files whose names pass {@code include} into
     * {@code root/}{@value #PRODUCTION_LAYOUT}, keeping package directories.
     */
    private static Path copyIntoProductionLayout(Path root, Predicate<String> include)
            throws IOException {
        Path fixtures = fixturesDirectory();
        Path destination = root.resolve(PRODUCTION_LAYOUT)
                .resolve(RegionCodes.class.getPackageName().replace('.', '/'));
        Files.createDirectories(destination);
        try (Stream<Path> classFiles = Files.list(fixtures)) {
            for (Path classFile : classFiles.filter(file -> include.test(file.getFileName().toString())).toList()) {
                Files.copy(classFile, destination.resolve(classFile.getFileName()));
            }
        }
        return root.resolve(PRODUCTION_LAYOUT);
    }

    private static Path fixturesDirectory() {
        try {
            return Path.of(classFileOf(RegionCodes.class).toURI()).getParent();
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
