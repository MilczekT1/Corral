package io.github.milczekt1.corral.rules.testing.nomutablestaticstate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.rules.testing.nomutablestaticstate.fixtures.BasketPricing;
import io.github.milczekt1.corral.rules.testing.nomutablestaticstate.fixtures.OrderCounting;
import io.github.milczekt1.corral.rules.testing.nomutablestaticstate.fixtures.PriceCapturing;
import io.github.milczekt1.corral.rules.testing.nomutablestaticstate.fixtures.TemporaryDirectoryHolder;
import io.github.milczekt1.corral.scope.TestScope;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The examples live in {@code fixtures/}, which Surefire and Sonar both exclude: one of them carries
 * {@code @TempDir}, and a nested class in a test file is analysed as a test of that file.
 */
class NoMutableStaticStateRuleTest {

    private static final String ID = "corral.test.no-mutable-static-state";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    /** {@code Locale} holds static non-final fields and is JDK-sourced: it pins the scope clause. */
    private static final JavaClasses EXAMPLES = new ClassFileImporter().importClasses(
            OrderCounting.class, PriceCapturing.class, BasketPricing.class,
            TemporaryDirectoryHolder.class, Locale.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static String report() {
        return String.join("\n", NoMutableStaticStateRule.DEFINITION
                .allowEmptyShould(true).evaluate(EXAMPLES).getFailureReport().getDetails());
    }

    static Stream<Arguments> flags() {
        return Stream.of(
                arguments("a static counter", "placedOrders"),
                arguments("a static collection field", "capturedPrices"));
    }

    @ParameterizedTest(name = "flags {0}")
    @MethodSource
    void flags(String description, String offender) {
        String report = report();

        assertTrue(report.contains(offender), () -> "must flag " + description + ": " + report);
    }

    static Stream<Arguments> ignores() {
        return Stream.of(
                arguments("a static final constant", "CURRENCY"),
                arguments("a static final field holding a mutable object", "KNOWN_SKUS"),
                arguments("a final instance field", "buyerId"),
                arguments("a @TempDir field JUnit has to assign", "exportDirectory"));
    }

    @ParameterizedTest(name = "ignores {0}")
    @MethodSource
    void ignores(String description, String absent) {
        String report = report();

        assertFalse(report.contains(absent), () -> "must ignore " + description + ": " + report);
    }

    /** Premises asserted, so a JDK that changed either fails loudly instead of unpinning the clause. */
    @Test
    void ignoresAStaticFieldInProductionCode() {
        JavaClass locale = EXAMPLES.get(Locale.class);

        assertFalse(TestScope.TEST_CLASSES.test(locale),
                "premise: a JDK class has no test-output source, so it is production-scoped");
        assertTrue(locale.getFields().stream()
                        .anyMatch(field -> field.getModifiers().contains(JavaModifier.STATIC)
                                && !field.getModifiers().contains(JavaModifier.FINAL)),
                "premise: Locale must still declare a static non-final field, or this pins nothing");

        assertFalse(report().contains("Locale"),
                "this rule is scoped to test classes, and Locale is not one: " + report());
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoMutableStaticStateRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(EXAMPLES);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("placedOrders"), debt);
            assertTrue(debt.contains("capturedPrices"), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
