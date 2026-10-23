package io.github.milczekt1.corral.rules.naming.lowercasepackages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LowercasePackagesRuleTest {

    private static final String ID = "corral.naming.packages-must-be-lowercase";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    private static final String FIXTURES = LowercasePackagesRuleTest.class.getPackageName() + ".fixtures";

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() {
        examples = new ClassFileImporter().importPackages(FIXTURES);

        assertEquals(10, examples.size(),
                "eight top-level examples plus OrderService's nested and anonymous classes must be imported,"
                        + " or the ignore assertions below are vacuous");
    }

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violations() {
        return LowercasePackagesRule.DEFINITION.evaluate(examples).getFailureReport().getDetails();
    }

    private static void assertFlagged(String className) {
        List<String> violations = violations();

        assertTrue(violations.stream().anyMatch(line -> line.startsWith("Class <" + FIXTURES + "." + className + ">")),
                String.join("\n", violations));
    }

    @Test
    void flagsACamelCaseSegment() {
        assertFlagged("orderProcessing.OrderService");
    }

    @Test
    void flagsACapitalisedSegment() {
        assertFlagged("Model.Invoice");
    }

    @Test
    void flagsUppercaseInASegmentBeforeTheLast() {
        assertFlagged("Acme.order.Shipment");
    }

    @Test
    void namesTheClassThePackageAndTheSourceFile() {
        assertTrue(violations().contains("Class <" + FIXTURES + ".Model.Invoice> resides in package <" + FIXTURES
                        + ".Model> in (Invoice.java:0)"),
                String.join("\n", violations()));
    }

    @Test
    void doesNotReportNestedOrAnonymousClasses() {
        String report = String.join("\n", violations());

        assertFalse(report.contains("OrderService$"), report);
    }

    @Test
    void reportsExactlyThreeClasses() {
        assertEquals(3, violations().size(), String.join("\n", violations()));
    }

    @ParameterizedTest(name = "ignores {0}")
    @ValueSource(strings = {"PaymentService", "Refund", "Payout", "Ledger", "Receipt"})
    void ignoresLowercaseUnderscoreAndDigitPackages(String example) {
        String report = String.join("\n", violations());

        assertFalse(report.contains(example), report);
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, LowercasePackagesRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(examples);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains(".orderProcessing.OrderService>"), debt);
            assertTrue(debt.contains(".Model.Invoice>"), debt);
            assertTrue(debt.contains(".Acme.order.Shipment>"), debt);
            assertEquals(3, debt.lines().count(), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
