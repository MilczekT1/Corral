package io.github.milczekt1.corral.rules.java.nooptionalfields;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.java.nooptionalfields.fixtures.OrderQuery;
import io.github.milczekt1.corral.rules.java.nooptionalfields.fixtures.TestOutputQuery;
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
 * fixture class files — nested and anonymous ones included — are copied into a Gradle main-output
 * layout and imported from there; {@link TestOutputQuery} alone is imported from where it compiled.
 */
class NoOptionalFieldsRuleTest {

    private static final String ID = "corral.java.no-optional-fields";

    private static final String RAW_TYPES = "has raw type Optional, OptionalInt, OptionalLong or OptionalDouble";

    /** Fifteen fixture sources, plus {@code ShippingService$Quote} and {@code ReminderTask$1}. */
    private static final int FIXTURE_CLASS_FILES = 17;

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        Path productionOutput = ProductionLayout.copyPackageOf(OrderQuery.class, outputRoot.resolve("examples"),
                List.of(TestOutputQuery.class));
        examples = new ClassFileImporter().importLocations(
                ProductionLayout.withTestOutput(productionOutput, TestOutputQuery.class));

        assertEquals(FIXTURE_CLASS_FILES, examples.size(),
                "every example must be imported, or the ignore assertions below are vacuous");
    }

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violationsIn(JavaClasses classes) {
        return NoOptionalFieldsRule.DEFINITION.evaluate(classes).getFailureReport().getDetails();
    }

    private static void assertFlagged(String field) {
        List<String> violations = violationsIn(examples);

        assertTrue(violations.stream().anyMatch(line -> line.contains("." + field + "> " + RAW_TYPES)),
                String.join("\n", violations));
    }

    private static void assertNotReported(String subject) {
        String report = String.join("\n", violationsIn(examples));

        assertFalse(report.contains("." + subject), report);
    }

    @Test
    void flagsAnOptionalField() {
        assertFlagged("OrderQuery.name");
    }

    @Test
    void flagsATransientOptionalField() {
        assertFlagged("CachedQuote.discount");
    }

    @Test
    void flagsAnOptionalRecordComponent() {
        assertFlagged("OrderView.note");
    }

    @Test
    void flagsAnOptionalIntField() {
        assertFlagged("Measurements.retries");
    }

    @Test
    void flagsAnOptionalLongField() {
        assertFlagged("Measurements.elapsed");
    }

    @Test
    void flagsAnOptionalDoubleField() {
        assertFlagged("Measurements.ratio");
    }

    @Test
    void flagsAConstructorInjectedFieldOfAClassWithoutAStereotype() {
        assertFlagged("FraudClient.fallbackClock");
    }

    @Test
    void flagsAFieldOfANamedBean() {
        assertFlagged("NamedLookup.lookupClock");
    }

    @Test
    void flagsAFieldOfAClassNestedInAStereotypedBean() {
        assertFlagged("ShippingService$Quote.carrier");
    }

    @Test
    void reportsOneViolationPerOptionalField() {
        List<String> violations = violationsIn(examples);

        assertEquals(9, violations.size(), String.join("\n", violations));
    }

    @Test
    void ignoresTheNullableFieldBesideAFlaggedOne() {
        assertNotReported("OrderQuery.reference");
    }

    @Test
    void ignoresAnOptionalTypeArgument() {
        assertNotReported("OrderQuery.tags");
    }

    @Test
    void ignoresThePlainRecordComponentBesideAFlaggedOne() {
        assertNotReported("OrderView.id");
    }

    @Test
    void ignoresOptionalReturnAndParameterTypes() {
        assertNotReported("PaymentQuery");
    }

    @Test
    void ignoresTheSyntheticFieldOfACapturedLocal() {
        assertNotReported("ReminderTask");
    }

    @Test
    void ignoresAStaticConstant() {
        assertNotReported("Defaults");
    }

    @Test
    void ignoresAConstructorInjectedFieldOfAService() {
        assertNotReported("RefundService");
    }

    @Test
    void ignoresAnAutowiredFieldOfAService() {
        assertNotReported("CheckoutService");
    }

    @Test
    void ignoresTheFieldOfTheServiceANestedClassIsDeclaredIn() {
        assertNotReported("ShippingService.tracker");
    }

    @Test
    void ignoresAFieldOfAConfiguration() {
        assertNotReported("PaymentConfig");
    }

    @Test
    void ignoresAComponentRecord() {
        assertNotReported("TariffTable");
    }

    @Test
    void ignoresAnOptionalFieldInTestOutput() {
        assertNotReported("TestOutputQuery");
    }

    /** The same class file as above, moved: only its location changed, so only scope can explain the difference. */
    @Test
    void flagsTheTestOutputFieldOnceItSitsInProductionOutput() throws IOException {
        Path productionOutput = ProductionLayout.copy(outputRoot.resolve("query"), List.of(TestOutputQuery.class));
        JavaClasses query = new ClassFileImporter().importPath(productionOutput);

        List<String> violations = violationsIn(query);

        assertTrue(violations.stream().anyMatch(line -> line.contains(".TestOutputQuery.keyword>")),
                String.join("\n", violations));
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoOptionalFieldsRule.rule, ID, examples);

        assertTrue(debt.contains(".OrderQuery.name> " + RAW_TYPES), debt);
        assertTrue(debt.contains(".Measurements.retries> " + RAW_TYPES), debt);
        assertTrue(debt.contains(".ShippingService$Quote.carrier> " + RAW_TYPES), debt);
    }
}
