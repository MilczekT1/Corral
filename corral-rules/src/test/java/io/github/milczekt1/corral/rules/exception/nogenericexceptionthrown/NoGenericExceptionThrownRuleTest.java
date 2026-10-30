package io.github.milczekt1.corral.rules.exception.nogenericexceptionthrown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.rules.exception.nogenericexceptionthrown.fixtures.CallerTrace;
import io.github.milczekt1.corral.rules.exception.nogenericexceptionthrown.fixtures.FailingGatewayStub;
import io.github.milczekt1.corral.rules.exception.nogenericexceptionthrown.fixtures.LeakTracker;
import io.github.milczekt1.corral.rules.exception.nogenericexceptionthrown.fixtures.LedgerImporter;
import io.github.milczekt1.corral.rules.exception.nogenericexceptionthrown.fixtures.OrderService;
import io.github.milczekt1.corral.rules.exception.nogenericexceptionthrown.fixtures.OutageDrillTest;
import io.github.milczekt1.corral.rules.exception.nogenericexceptionthrown.fixtures.PaymentDeclinedException;
import io.github.milczekt1.corral.rules.exception.nogenericexceptionthrown.fixtures.PaymentService;
import io.github.milczekt1.corral.rules.exception.nogenericexceptionthrown.fixtures.SchemaVersions;
import io.github.milczekt1.corral.rules.testsupport.ProductionLayout;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The rule is scoped to production classes, and every fixture compiles into test output. So the
 * examples are copied into a Gradle main-output layout and imported from there;
 * {@link FailingGatewayStub} alone is imported from where it compiled.
 */
class NoGenericExceptionThrownRuleTest {

    private static final String ID = "corral.exception.no-generic-exception-thrown";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    private static final List<Class<?>> PRODUCTION_EXAMPLES = List.of(
            OrderService.class, LedgerImporter.class, SchemaVersions.class, LeakTracker.class,
            PaymentDeclinedException.class, CallerTrace.class, PaymentService.class, OutageDrillTest.class);

    /** {@link OrderService}'s anonymous {@code RuntimeException}. */
    private static final int ANONYMOUS_EXAMPLES = 1;

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        Path productionOutput = ProductionLayout.copy(outputRoot.resolve("examples"), PRODUCTION_EXAMPLES);
        examples = new ClassFileImporter().importLocations(List.of(
                Location.of(productionOutput),
                Location.of(ProductionLayout.classFileOf(FailingGatewayStub.class))));

        assertEquals(PRODUCTION_EXAMPLES.size() + ANONYMOUS_EXAMPLES + 1, examples.size(),
                "every example must be imported, or the ignore assertions below are vacuous");
    }

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violationsIn(JavaClasses classes) {
        return NoGenericExceptionThrownRule.DEFINITION.evaluate(classes).getFailureReport().getDetails();
    }

    private static void assertFlagged(String caller, String access) {
        List<String> violations = violationsIn(examples);

        assertTrue(violations.stream().anyMatch(line -> line.contains(caller + "(") && line.contains(access)),
                String.join("\n", violations));
    }

    private static void assertNotReported(String subject) {
        String report = String.join("\n", violationsIn(examples));

        assertFalse(report.contains(subject), report);
    }

    @Test
    void flagsARuntimeException() {
        assertFlagged("OrderService.byId",
                "calls constructor <java.lang.RuntimeException.<init>(java.lang.String)>");
    }

    @Test
    void flagsAnException() {
        assertFlagged("LedgerImporter.importFile", "calls constructor <java.lang.Exception.<init>(java.lang.String)>");
    }

    @Test
    void flagsAnError() {
        assertFlagged("SchemaVersions.migrate", "calls constructor <java.lang.Error.<init>(java.lang.String)>");
    }

    @Test
    void flagsAThrowableAssignedToAFieldInAConstructor() {
        assertFlagged(LeakTracker.class.getName() + ".<init>",
                "calls constructor <java.lang.Throwable.<init>(java.lang.String)>");
    }

    @Test
    void flagsARuntimeExceptionConstructorReference() {
        assertFlagged("OrderService.firstLine", "references constructor <java.lang.RuntimeException.<init>()>");
    }

    @Test
    void flagsAStaticFactoryInsideASubclass() {
        assertFlagged("PaymentDeclinedException.wrapping",
                "calls constructor <java.lang.RuntimeException.<init>(java.lang.String)>");
    }

    @Test
    void flagsAConstructorReferenceInsideASubclassConstructor() {
        assertFlagged(PaymentDeclinedException.class.getName() + ".<init>",
                "references constructor <java.lang.RuntimeException.<init>()>");
    }

    @Test
    void flagsAnAnonymousSubclassOnItsOwnConstructor() {
        assertFlagged(OrderService.class.getName() + "$1.<init>",
                "calls constructor <java.lang.RuntimeException.<init>(java.lang.String)>");
    }

    @Test
    void reportsExactlyEightConstructions() {
        assertEquals(8, violationsIn(examples).size(), String.join("\n", violationsIn(examples)));
    }

    @Test
    void ignoresTheMethodThrowingAnAnonymousSubclass() {
        assertNotReported("OrderService.byReference(");
    }

    @Test
    void ignoresMethodsCalledOnAGenericThrowableBesideFlaggedConstructions() {
        assertNotReported("OrderService.reasonOf(");
        assertNotReported("LeakTracker.allocatedAt(");
    }

    @Test
    void ignoresAnIllegalArgumentExceptionBesideAFlaggedRuntimeException() {
        assertNotReported("OrderService.validate(");
    }

    @Test
    void ignoresAnAssertionErrorBesideAFlaggedError() {
        assertNotReported("SchemaVersions.columnsIn(");
    }

    @Test
    void ignoresASubclassCallingItsSuperclassConstructor() {
        assertNotReported("PaymentDeclinedException.<init>(java.lang.String)");
    }

    @Test
    void ignoresADirectThrowableSubclassCallingItsSuperclassConstructor() {
        assertNotReported("CallerTrace");
    }

    @Test
    void ignoresThrowingANamedSubclassAndRethrowingACaughtOne() {
        assertNotReported("PaymentService");
    }

    @Test
    void ignoresAClassDeclaringATestMethodInProductionOutput() {
        assertNotReported("OutageDrillTest");
    }

    @Test
    void ignoresAGenericExceptionConstructedInTestOutput() {
        assertNotReported("FailingGatewayStub");
    }

    /** The same class file as above, moved: only its location changed, so only scope can explain the difference. */
    @Test
    void flagsTheTestOutputStubOnceItSitsInProductionOutput() throws IOException {
        Path productionOutput = ProductionLayout.copy(outputRoot.resolve("stub"), List.of(FailingGatewayStub.class));
        JavaClasses stub = new ClassFileImporter().importPath(productionOutput);

        List<String> violations = violationsIn(stub);

        assertTrue(violations.stream().anyMatch(line -> line.contains("FailingGatewayStub.fetch(")),
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoGenericExceptionThrownRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(examples);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("OrderService.byId("), debt);
            assertTrue(debt.contains("OrderService.firstLine("), debt);
            assertTrue(debt.contains("OrderService$1.<init>("), debt);
            assertTrue(debt.contains("LedgerImporter.importFile("), debt);
            assertTrue(debt.contains("SchemaVersions.migrate("), debt);
            assertTrue(debt.contains("LeakTracker.<init>()> calls"), debt);
            assertTrue(debt.contains("PaymentDeclinedException.wrapping("), debt);
            assertTrue(debt.contains("PaymentDeclinedException.<init>(java.util.Optional)> references"), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
