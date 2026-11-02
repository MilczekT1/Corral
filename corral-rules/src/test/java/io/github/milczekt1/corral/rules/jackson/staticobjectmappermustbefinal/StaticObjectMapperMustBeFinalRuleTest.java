package io.github.milczekt1.corral.rules.jackson.staticobjectmappermustbefinal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.jackson.staticobjectmappermustbefinal.fixtures.JsonUtils;
import io.github.milczekt1.corral.rules.jackson.staticobjectmappermustbefinal.fixtures.TestOutputJson;
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
 * fixture class files are copied into a Gradle main-output layout and imported from there;
 * {@link TestOutputJson} alone is imported from where it compiled.
 */
class StaticObjectMapperMustBeFinalRuleTest {

    private static final String ID = "corral.jackson.static-object-mapper-must-be-final";

    private static final String NOT_FINAL = "> does not have modifier FINAL in (";

    private static final int FIXTURE_CLASS_FILES = 8;

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        Path productionOutput = ProductionLayout.copyPackageOf(JsonUtils.class, outputRoot.resolve("examples"),
                List.of(TestOutputJson.class));
        examples = new ClassFileImporter().importLocations(
                ProductionLayout.withTestOutput(productionOutput, TestOutputJson.class));

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
        Path productionOutput = ProductionLayout.copy(outputRoot.resolve("json"), List.of(TestOutputJson.class));
        JavaClasses json = new ClassFileImporter().importPath(productionOutput);

        List<String> violations = violationsIn(json);

        assertTrue(violations.stream().anyMatch(line -> line.contains(".TestOutputJson.overrideMapper" + NOT_FINAL)),
                String.join("\n", violations));
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(StaticObjectMapperMustBeFinalRule.rule, ID, examples);

        assertTrue(debt.contains(".JsonUtils.mapper" + NOT_FINAL), debt);
        assertTrue(debt.contains(".ReportExporter.jsonMapper" + NOT_FINAL), debt);
        assertTrue(debt.contains(".EventCodec.codecMapper" + NOT_FINAL), debt);
        assertTrue(debt.contains(".AuditLog.auditMapper" + NOT_FINAL), debt);
        assertTrue(debt.contains(".LegacyJsonConfig.MAPPER" + NOT_FINAL), debt);
    }
}
