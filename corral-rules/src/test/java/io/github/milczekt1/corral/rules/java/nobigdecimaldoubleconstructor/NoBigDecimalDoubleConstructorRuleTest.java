package io.github.milczekt1.corral.rules.java.nobigdecimaldoubleconstructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.rules.java.nobigdecimaldoubleconstructor.fixtures.ExchangeRate;
import io.github.milczekt1.corral.rules.java.nobigdecimaldoubleconstructor.fixtures.RatePricing;
import io.github.milczekt1.corral.rules.java.nobigdecimaldoubleconstructor.fixtures.RateStreams;
import io.github.milczekt1.corral.rules.java.nobigdecimaldoubleconstructor.fixtures.SafePricing;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoBigDecimalDoubleConstructorRuleTest {

    private static final String ID = "corral.java.no-bigdecimal-double-constructor";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    private static final JavaClasses EXAMPLES = new ClassFileImporter().importClasses(
            RatePricing.class, RateStreams.class, SafePricing.class, ExchangeRate.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violations() {
        return NoBigDecimalDoubleConstructorRule.DEFINITION.evaluate(EXAMPLES).getFailureReport().getDetails();
    }

    private static void assertFlagged(String caller, String constructor) {
        List<String> violations = violations();

        assertTrue(violations.stream().anyMatch(line -> line.contains(caller) && line.contains(constructor)),
                String.join("\n", violations));
    }

    private static void assertNotReported(String subject) {
        String report = String.join("\n", violations());

        assertFalse(report.contains(subject), report);
    }

    @Test
    void flagsTheDoubleConstructorOnALiteral() {
        assertFlagged("RatePricing.<clinit>()", "calls constructor <java.math.BigDecimal.<init>(double)>");
    }

    @Test
    void flagsTheDoubleConstructorOnAField() {
        assertFlagged("RatePricing.fromField()", "calls constructor <java.math.BigDecimal.<init>(double)>");
    }

    @Test
    void flagsAFloatWidenedToTheDoubleConstructor() {
        assertFlagged("RatePricing.fromFloat()", "calls constructor <java.math.BigDecimal.<init>(double)>");
    }

    @Test
    void flagsTheConstructorTakingADoubleAndAMathContext() {
        assertFlagged("RatePricing.withContext(double)",
                "calls constructor <java.math.BigDecimal.<init>(double, java.math.MathContext)>");
    }

    @Test
    void flagsAConstructorReference() {
        assertFlagged("RateStreams.referenced(", "references constructor <java.math.BigDecimal.<init>(double)>");
    }

    @Test
    void flagsAConstructorCallInsideALambda() {
        assertFlagged("RateStreams.wrapped(", "calls constructor <java.math.BigDecimal.<init>(double)>");
    }

    @Test
    void reportsExactlySixAccesses() {
        assertEquals(6, violations().size(), String.join("\n", violations()));
    }

    @Test
    void ignoresTheStringConstructorBesideFlaggedCalls() {
        assertNotReported("fromText");
    }

    @Test
    void ignoresAValueOfReference() {
        assertNotReported("RateStreams.converted");
    }

    @Test
    void ignoresValueOfTheStringConstructorAndIntegralConstructors() {
        assertNotReported("SafePricing");
    }

    @Test
    void ignoresADoubleConstructorOfAnotherType() {
        assertNotReported("ExchangeRate");
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoBigDecimalDoubleConstructorRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(EXAMPLES);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("RatePricing.<clinit>("), debt);
            assertTrue(debt.contains("RatePricing.fromField("), debt);
            assertTrue(debt.contains("RatePricing.fromFloat("), debt);
            assertTrue(debt.contains("RatePricing.withContext("), debt);
            assertTrue(debt.contains("RateStreams.referenced("), debt);
            assertTrue(debt.contains("RateStreams.wrapped("), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
