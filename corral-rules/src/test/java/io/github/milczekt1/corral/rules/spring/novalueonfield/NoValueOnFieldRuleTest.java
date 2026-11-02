package io.github.milczekt1.corral.rules.spring.novalueonfield;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.spring.novalueonfield.fixtures.OrderService;
import io.github.milczekt1.corral.rules.spring.novalueonfield.fixtures.TestOutputPortHolder;
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
 * fixture class files are copied into a Gradle main-output layout and imported from there; the two
 * JUnit test classes among them are then test classes only by their declared test methods.
 * {@link TestOutputPortHolder} alone is imported from where it compiled.
 */
class NoValueOnFieldRuleTest {

    private static final String ID = "corral.spring.no-value-on-field";

    private static final String META_ANNOTATED = "> is meta-annotated with @Value";

    private static final int FIXTURE_CLASS_FILES = 12;

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        Path productionOutput = ProductionLayout.copyPackageOf(OrderService.class, outputRoot.resolve("examples"),
                List.of(TestOutputPortHolder.class));
        examples = new ClassFileImporter().importLocations(
                ProductionLayout.withTestOutput(productionOutput, TestOutputPortHolder.class));

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
        Path productionOutput = ProductionLayout.copy(outputRoot.resolve("holder"),
                List.of(TestOutputPortHolder.class));
        JavaClasses holder = new ClassFileImporter().importPath(productionOutput);

        List<String> violations = violationsIn(holder);

        assertTrue(violations.stream().anyMatch(line -> line.contains(".TestOutputPortHolder.port" + META_ANNOTATED)),
                String.join("\n", violations));
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoValueOnFieldRule.rule, ID, examples);

        assertTrue(debt.contains(".OrderService.timeout" + META_ANNOTATED), debt);
        assertTrue(debt.contains(".CacheSettings.MAX_ENTRIES" + META_ANNOTATED), debt);
        assertTrue(debt.contains(".ShippingService.timeout" + META_ANNOTATED), debt);
    }
}
