package io.github.milczekt1.corral.rules.testing.nodisabledwithoutreason;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.rules.testing.nodisabledwithoutreason.fixtures.DeliveryScheduling;
import io.github.milczekt1.corral.rules.testing.nodisabledwithoutreason.fixtures.InvoiceRendering;
import io.github.milczekt1.corral.rules.testing.nodisabledwithoutreason.fixtures.OrderPlacement;
import io.github.milczekt1.corral.rules.testing.nodisabledwithoutreason.fixtures.PendingFix;
import io.github.milczekt1.corral.rules.testing.nodisabledwithoutreason.fixtures.PendingMigration;
import io.github.milczekt1.corral.rules.testing.nodisabledwithoutreason.fixtures.ReceiptWriting;
import io.github.milczekt1.corral.rules.testing.nodisabledwithoutreason.fixtures.ShipmentTracking;
import io.github.milczekt1.corral.scope.TestScope;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The examples live in {@code fixtures/}, which Surefire and Sonar both exclude: a bare
 * {@code @Disabled} is the violation under test, and anywhere else it would be a disabled test in
 * this very suite and a linter would be told to delete it.
 */
class NoDisabledWithoutReasonRuleTest {

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

    static Stream<Arguments> flags() {
        return Stream.of(
                arguments("a bare @Disabled on a method", "placesTheOrder"),
                arguments("a bare @Disabled on a class", "ReceiptWriting"),
                arguments("an empty reason", "tracksTheParcel"),
                arguments("a whitespace-only reason", "notifiesTheCustomer"),
                arguments("a composed annotation at its declaration", "PendingFix"));
    }

    @ParameterizedTest(name = "flags {0}")
    @MethodSource
    void flags(String description, String offender) {
        String report = report();

        assertTrue(report.contains(offender), () -> "must flag " + description + ": " + report);
    }

    static Stream<Arguments> ignores() {
        return Stream.of(
                arguments("a stated reason on a method", "refundsTheOrder"),
                arguments("a stated reason on a class", "DeliveryScheduling"),
                arguments("a stated reason on a composed annotation", "PendingMigration"),
                arguments("a method that is not disabled at all", "reservesStock"),
                arguments("a method inside a disabled class", "writesTheReceipt"),
                arguments("the conditional @DisabledOnOs", "schedulesTheDelivery"),
                arguments("the conditional @DisabledIfEnvironmentVariable", "retriesTheDelivery"));
    }

    @ParameterizedTest(name = "ignores {0}")
    @MethodSource
    void ignores(String description, String absent) {
        String report = report();

        assertFalse(report.contains(absent), () -> "must ignore " + description + ": " + report);
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
