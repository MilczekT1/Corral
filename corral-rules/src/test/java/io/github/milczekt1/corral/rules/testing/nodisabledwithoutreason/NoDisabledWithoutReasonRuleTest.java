package io.github.milczekt1.corral.rules.testing.nodisabledwithoutreason;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.scope.TestScope;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * The examples are nested and static, so no runner selects them — a top-level {@code *IT} outside a
 * {@code fixtures} package is run by Failsafe for real, and a top-level {@code *Test} carrying a
 * bare {@code @Disabled} would be a disabled test in this very suite.
 */
class NoDisabledWithoutReasonRuleTest {

    /**
     * The enabled method and the explained one are load-bearing: {@code alwaysTrue()} would pass
     * without them.
     */
    static class OrderPlacement {

        @Disabled
        @Test
        void placesTheOrder() {
        }

        @Test
        void reservesStock() {
        }

        @Disabled("Blocked on gateway sandbox outage, see #412 — re-enable when it closes")
        @Test
        void refundsTheOrder() {
        }
    }

    @Disabled
    static class ReceiptWriting {

        @Test
        void writesTheReceipt() {
        }
    }

    static class ShipmentTracking {

        @Disabled("")
        @Test
        void tracksTheParcel() {
        }

        @Disabled("   ")
        @Test
        void notifiesTheCustomer() {
        }
    }

    @Disabled("Blocked on the carrier sandbox, see #900 — re-enable when it returns")
    static class DeliveryScheduling {

        @DisabledOnOs(OS.WINDOWS)
        @Test
        void schedulesTheDelivery() {
        }

        @DisabledIfEnvironmentVariable(named = "CI", matches = "true")
        @Test
        void retriesTheDelivery() {
        }
    }

    @Disabled
    @Retention(RetentionPolicy.RUNTIME)
    @interface PendingFix {
    }

    @Disabled("Pending the 2026 pricing migration, see #900")
    @Retention(RetentionPolicy.RUNTIME)
    @interface PendingMigration {
    }

    static class InvoiceRendering {

        @PendingFix
        @Test
        void rendersTheInvoice() {
        }
    }

    private static final String ID = "corral.test.junit.no-disabled-without-reason";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    private static final JavaClasses EXAMPLES = new ClassFileImporter().importClasses(
            OrderPlacement.class, ReceiptWriting.class, ShipmentTracking.class,
            DeliveryScheduling.class, PendingFix.class, PendingMigration.class,
            InvoiceRendering.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static String report() {
        return String.join("\n", NoDisabledWithoutReasonRule.DEFINITION
                .allowEmptyShould(true).evaluate(EXAMPLES).getFailureReport().getDetails());
    }

    @Test
    void flagsABareDisabledOnAMethod() {
        String report = report();

        assertTrue(report.contains("placesTheOrder"), report);
    }

    @Test
    void flagsABareDisabledOnAClass() {
        String report = report();

        assertTrue(report.contains("ReceiptWriting"), report);
    }

    @Test
    void flagsAnEmptyAndAWhitespaceOnlyReason() {
        String report = report();

        assertTrue(report.contains("tracksTheParcel"), report);
        assertTrue(report.contains("notifiesTheCustomer"), report);
    }

    @Test
    void ignoresADisabledThatStatesAReason() {
        String report = report();

        assertFalse(report.contains("refundsTheOrder"), report);
        assertFalse(report.contains("DeliveryScheduling"), report);
        assertFalse(report.contains("PendingMigration"), report);
    }

    @Test
    void ignoresAMethodThatIsNotDisabledAtAll() {
        String report = report();

        assertFalse(report.contains("reservesStock"), report);
        assertFalse(report.contains("writesTheReceipt"), report);
    }

    @Test
    void ignoresTheConditionalDisableAnnotations() {
        String report = report();

        assertFalse(report.contains("schedulesTheDelivery"), report);
        assertFalse(report.contains("retriesTheDelivery"), report);
    }

    @Test
    void flagsTheDeclarationOfAComposedAnnotationHidingABareDisabled() {
        String report = report();

        assertTrue(report.contains("PendingFix"), report);
    }

    @Test
    void ignoresAUseOfAComposedAnnotation() {
        assertTrue(EXAMPLES.get(InvoiceRendering.class)
                        .getMethod("rendersTheInvoice")
                        .isMetaAnnotatedWith(NoDisabledWithoutReasonRule.DISABLED),
                "premise: the use must really resolve to @Disabled through @PendingFix");

        assertFalse(report().contains("InvoiceRendering"), report());
    }

    /**
     * Every class this module compiles lands in test output, so no example can be production-scoped:
     * the scope is asserted through the description, which is also the freeze-store key's wording.
     */
    @Test
    void scopesItselfToTestClasses() {
        JavaClass productionClass = new ClassFileImporter().importClasses(String.class).get(String.class);

        assertFalse(TestScope.TEST_CLASSES.test(productionClass),
                "premise: a JDK class has no test-output source, so it is production-scoped");

        String description = NoDisabledWithoutReasonRule.DEFINITION.getDescription();
        assertTrue(description.contains("test classes"), description);
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoDisabledWithoutReasonRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(EXAMPLES);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("placesTheOrder"), debt);
            assertTrue(debt.contains("ReceiptWriting"), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
