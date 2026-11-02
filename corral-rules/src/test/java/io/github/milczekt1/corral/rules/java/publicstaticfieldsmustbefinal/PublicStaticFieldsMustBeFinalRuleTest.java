package io.github.milczekt1.corral.rules.java.publicstaticfieldsmustbefinal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.java.publicstaticfieldsmustbefinal.fixtures.TestOutputSettings;
import io.github.milczekt1.corral.rules.java.publicstaticfieldsmustbefinal.fixtures.production.RuntimeSettings;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import io.github.milczekt1.corral.rules.testsupport.FixtureVerdicts;
import io.github.milczekt1.corral.rules.testsupport.ProductionLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PublicStaticFieldsMustBeFinalRuleTest {

    private static final String ID = "corral.java.public-static-fields-must-be-final";

    private static final String NOT_FINAL = "> does not have modifier FINAL in (";

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        examples = ProductionLayout.importFixtures(PublicStaticFieldsMustBeFinalRuleTest.class, outputRoot.resolve("examples"));
    }

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violationsIn(JavaClasses classes) {
        return PublicStaticFieldsMustBeFinalRule.DEFINITION.evaluate(classes).getFailureReport().getDetails();
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
    void flagsANonFinalPublicStaticField() {
        assertFlagged("RuntimeSettings.endpoint");
    }

    @Test
    void flagsANonFinalPublicStaticFieldOfANestedClass() {
        assertFlagged("RuntimeSettings$Limits.ceiling");
    }

    @Test
    void flagsAVolatileFieldOutsideAStaticMetamodel() {
        assertFlagged("MetricsRegistry.lastSnapshot");
    }

    @Test
    void flagsAPublicStaticFieldOfAPackagePrivateClass() {
        assertFlagged("InternalCounters.hits");
    }

    @Test
    void reportsOneViolationPerField() {
        List<String> violations = violationsIn(examples);

        assertEquals(4, violations.size(), String.join("\n", violations));
    }

    @Test
    void ignoresAPublicStaticFinalConstant() {
        assertNotReported("RuntimeSettings.NAME");
    }

    @Test
    void ignoresAFinalArrayConstant() {
        assertNotReported("RuntimeSettings.FALLBACK_HOSTS");
    }

    @Test
    void ignoresAPrivateStaticField() {
        assertNotReported("RuntimeSettings.cache");
    }

    @Test
    void ignoresAPackagePrivateStaticField() {
        assertNotReported("RuntimeSettings.packageLevel");
    }

    @Test
    void ignoresAProtectedStaticField() {
        assertNotReported("RuntimeSettings.inherited");
    }

    @Test
    void ignoresAPublicInstanceField() {
        assertNotReported("RuntimeSettings.label");
    }

    @Test
    void ignoresAJakartaStaticMetamodel() {
        assertNotReported("Shipment_");
    }

    @Test
    void ignoresAJavaxStaticMetamodel() {
        assertNotReported("Voucher_");
    }

    @Test
    void ignoresAFieldInTestOutput() {
        assertNotReported("TestOutputSettings");
    }

    @Test
    void flagsTheTestOutputFieldOnceItSitsInProductionOutput() throws IOException {
        Path productionOutput = ProductionLayout.copy(outputRoot.resolve("settings"),
                List.of(TestOutputSettings.class));
        JavaClasses settings = new ClassFileImporter().importPath(productionOutput);

        List<String> violations = violationsIn(settings);

        assertTrue(violations.stream().anyMatch(line -> line.contains(".TestOutputSettings.overrideUrl" + NOT_FINAL)),
                String.join("\n", violations));
    }

    @Test
    void everyFixtureMatchesItsVerdict() {
        FixtureVerdicts.assertMatch(PublicStaticFieldsMustBeFinalRuleTest.class, PublicStaticFieldsMustBeFinalRule.DEFINITION, examples);
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(PublicStaticFieldsMustBeFinalRule.rule, ID, examples);

        assertTrue(debt.contains(".RuntimeSettings.endpoint" + NOT_FINAL), debt);
        assertTrue(debt.contains(".RuntimeSettings$Limits.ceiling" + NOT_FINAL), debt);
        assertTrue(debt.contains(".MetricsRegistry.lastSnapshot" + NOT_FINAL), debt);
        assertTrue(debt.contains(".InternalCounters.hits" + NOT_FINAL), debt);
    }
}
