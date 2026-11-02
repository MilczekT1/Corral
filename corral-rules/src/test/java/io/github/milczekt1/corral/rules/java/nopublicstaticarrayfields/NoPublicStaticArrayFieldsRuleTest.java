package io.github.milczekt1.corral.rules.java.nopublicstaticarrayfields;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.java.nopublicstaticarrayfields.fixtures.TestOutputArrays;
import io.github.milczekt1.corral.rules.java.nopublicstaticarrayfields.fixtures.production.RegionCodes;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import io.github.milczekt1.corral.rules.testsupport.ProductionLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NoPublicStaticArrayFieldsRuleTest {

    private static final String ID = "corral.java.no-public-static-array-fields";

    private static final String ARRAY = "> has raw type an array in (";

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        examples = ProductionLayout.importFixtures(NoPublicStaticArrayFieldsRuleTest.class, outputRoot.resolve("examples"));
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
        Path productionOutput = ProductionLayout.copy(outputRoot.resolve("arrays"), List.of(TestOutputArrays.class));
        JavaClasses arrays = new ClassFileImporter().importPath(productionOutput);

        List<String> violations = violationsIn(arrays);

        assertTrue(violations.stream().anyMatch(line -> line.contains(".TestOutputArrays.FIXTURE_IDS" + ARRAY)),
                String.join("\n", violations));
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoPublicStaticArrayFieldsRule.rule, ID, examples);

        assertTrue(debt.contains(".RegionCodes.ISO_CODES" + ARRAY), debt);
        assertTrue(debt.contains(".RegionCodes.EMPTY" + ARRAY), debt);
        assertTrue(debt.contains(".RetryCounters.attempts" + ARRAY), debt);
        assertTrue(debt.contains(".CurrencySymbols.SYMBOLS" + ARRAY), debt);
    }
}
