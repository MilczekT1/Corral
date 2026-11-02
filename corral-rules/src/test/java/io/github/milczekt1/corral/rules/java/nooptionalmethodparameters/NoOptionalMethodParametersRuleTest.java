package io.github.milczekt1.corral.rules.java.nooptionalmethodparameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.java.nooptionalmethodparameters.fixtures.TestOutputLookup;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import io.github.milczekt1.corral.rules.testsupport.FixtureVerdicts;
import io.github.milczekt1.corral.rules.testsupport.ProductionLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NoOptionalMethodParametersRuleTest {

    private static final String ID = "corral.java.no-optional-method-parameters";

    private static final String RAW_TYPES =
            "has raw parameter types containing Optional, OptionalInt, OptionalLong or OptionalDouble";

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        examples = ProductionLayout.importFixtures(NoOptionalMethodParametersRuleTest.class, outputRoot.resolve("examples"));
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
        Path productionOutput = ProductionLayout.copy(outputRoot.resolve("lookup"), List.of(TestOutputLookup.class));

        List<String> violations = violationsIn(new ClassFileImporter().importPath(productionOutput));

        assertTrue(violations.stream().anyMatch(line -> line.contains(".TestOutputLookup.apply(")),
                String.join("\n", violations));
    }

    @Test
    void everyFixtureMatchesItsVerdict() {
        FixtureVerdicts.assertMatch(NoOptionalMethodParametersRuleTest.class, NoOptionalMethodParametersRule.DEFINITION, examples);
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoOptionalMethodParametersRule.rule, ID, examples);

        assertTrue(debt.contains(".OrderQuery.find(java.util.Optional)> " + RAW_TYPES), debt);
        assertTrue(debt.contains(".Thresholds.withRetries(java.util.OptionalInt)> " + RAW_TYPES), debt);
        assertTrue(debt.contains(".PresenceComparator.compare(java.util.Optional, java.util.Optional)> " + RAW_TYPES),
                debt);
    }
}
