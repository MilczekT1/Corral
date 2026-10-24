package io.github.milczekt1.corral.rules.jackson.staticobjectmappermustbefinal;

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
import io.github.milczekt1.corral.rules.jackson.staticobjectmappermustbefinal.fixtures.JsonUtils;
import io.github.milczekt1.corral.rules.jackson.staticobjectmappermustbefinal.fixtures.TestOutputJson;
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
 * {@link TestOutputJson} alone is imported from where it compiled.
 */
class StaticObjectMapperMustBeFinalRuleTest {

    private static final String ID = "corral.jackson.static-object-mapper-must-be-final";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    /** Matched by none of {@code TestScope}'s test-output patterns. */
    private static final String PRODUCTION_LAYOUT = "build/classes/java/main";

    private static final String NOT_FINAL = "> does not have modifier FINAL in (";

    private static final int FIXTURE_CLASS_FILES = 8;

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        Path productionOutput = copyIntoProductionLayout(outputRoot.resolve("examples"),
                name -> !name.startsWith(TestOutputJson.class.getSimpleName()));
        examples = new ClassFileImporter().importLocations(List.of(
                Location.of(productionOutput),
                Location.of(classFileOf(TestOutputJson.class))));

        assertEquals(FIXTURE_CLASS_FILES, examples.size(),
                "every example must be imported, or the ignore assertions below are vacuous");
    }

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violationsIn(JavaClasses classes) {
        return StaticObjectMapperMustBeFinalRule.DEFINITION.evaluate(classes).getFailureReport().getDetails();
    }

    private static void assertFlagged(String field) {
        List<String> violations = violationsIn(examples);

        assertTrue(violations.stream().anyMatch(line -> line.contains("." + field + NOT_FINAL)),
                String.join("\n", violations));
    }

    private static void assertNotReported(String subject) {
        String report = String.join("\n", violationsIn(examples));

        assertFalse(report.contains("." + subject), report);
    }

    @Test
    void flagsALazilyInitialisedJackson2ObjectMapper() {
        assertFlagged("JsonUtils.mapper");
    }

    @Test
    void flagsAJackson2JsonMapper() {
        assertFlagged("ReportExporter.jsonMapper");
    }

    @Test
    void flagsAJackson3JsonMapper() {
        assertFlagged("EventCodec.codecMapper");
    }

    @Test
    void flagsAProjectsOwnObjectMapperSubclass() {
        assertFlagged("AuditLog.auditMapper");
    }

    @Test
    void flagsAStaticFieldWrittenByAnInjectedSetter() {
        assertFlagged("LegacyJsonConfig.MAPPER");
    }

    @Test
    void reportsOneViolationPerField() {
        List<String> violations = violationsIn(examples);

        assertEquals(5, violations.size(), String.join("\n", violations));
    }

    @Test
    void ignoresAStaticObjectReader() {
        assertNotReported("JsonUtils.reader");
    }

    @Test
    void ignoresAStaticJackson3ObjectReader() {
        assertNotReported("EventCodec.eventReader");
    }

    @Test
    void ignoresAStaticObjectWriter() {
        assertNotReported("JsonUtils.prettyWriter");
    }

    @Test
    void ignoresAStaticFinalAtomicReferenceHoldingAMapper() {
        assertNotReported("JsonUtils.current");
    }

    @Test
    void ignoresAStaticObjectField() {
        assertNotReported("JsonUtils.untyped");
    }

    @Test
    void ignoresAStaticFinalJackson3Mapper() {
        assertNotReported("EventCodec.SHARED");
    }

    @Test
    void ignoresAStaticFinalMapperAndInstanceFields() {
        assertNotReported("PaymentService");
    }

    @Test
    void ignoresANonMapperStaticFieldOfTheMapperSubclass() {
        assertNotReported("AuditMapper");
    }

    @Test
    void ignoresAFieldInTestOutput() {
        assertNotReported("TestOutputJson");
    }

    @Test
    void flagsTheTestOutputFieldOnceItSitsInProductionOutput() throws IOException {
        Path productionOutput = copyIntoProductionLayout(outputRoot.resolve("json"),
                name -> name.equals(TestOutputJson.class.getSimpleName() + ".class"));
        JavaClasses json = new ClassFileImporter().importPath(productionOutput);

        List<String> violations = violationsIn(json);

        assertTrue(violations.stream().anyMatch(line -> line.contains(".TestOutputJson.overrideMapper" + NOT_FINAL)),
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, StaticObjectMapperMustBeFinalRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(examples);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains(".JsonUtils.mapper" + NOT_FINAL), debt);
            assertTrue(debt.contains(".ReportExporter.jsonMapper" + NOT_FINAL), debt);
            assertTrue(debt.contains(".EventCodec.codecMapper" + NOT_FINAL), debt);
            assertTrue(debt.contains(".AuditLog.auditMapper" + NOT_FINAL), debt);
            assertTrue(debt.contains(".LegacyJsonConfig.MAPPER" + NOT_FINAL), debt);
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
                .resolve(JsonUtils.class.getPackageName().replace('.', '/'));
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
            return Path.of(classFileOf(JsonUtils.class).toURI()).getParent();
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
