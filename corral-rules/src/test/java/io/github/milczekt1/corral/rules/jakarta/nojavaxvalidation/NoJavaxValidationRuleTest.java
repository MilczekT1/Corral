package io.github.milczekt1.corral.rules.jakarta.nojavaxvalidation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.jakarta.nojavaxvalidation.fixtures.HalfMigratedOrder;
import io.github.milczekt1.corral.rules.jakarta.nojavaxvalidation.fixtures.HandCheckedRefund;
import io.github.milczekt1.corral.rules.jakarta.nojavaxvalidation.fixtures.JakartaPayment;
import io.github.milczekt1.corral.rules.jakarta.nojavaxvalidation.fixtures.JavaxReferenceValidator;
import io.github.milczekt1.corral.rules.jakarta.nojavaxvalidation.fixtures.ViolationRethrower;
import io.github.milczekt1.corral.rules.jakarta.nojavaxvalidation.fixtures.ViolationSwallower;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import io.github.milczekt1.corral.rules.testsupport.FixtureVerdicts;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The examples live in {@code fixtures/}, which Surefire and Sonar both exclude: they are
 * deliberately bad code, and a linter told to fix any of it would delete the violation under test.
 */
class NoJavaxValidationRuleTest {

    private static final String ID = "corral.jakarta.no-javax-validation";

    private static final JavaClasses EXAMPLES = new ClassFileImporter().importClasses(
            HalfMigratedOrder.class, JavaxReferenceValidator.class, ViolationRethrower.class,
            ViolationSwallower.class, JakartaPayment.class, HandCheckedRefund.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static String report() {
        return String.join("\n", NoJavaxValidationRule.DEFINITION
                .evaluate(EXAMPLES).getFailureReport().getDetails());
    }

    @Test
    void flagsAJavaxConstraintAnnotation() {
        String report = report();

        assertTrue(report.contains("HalfMigratedOrder") && report.contains("javax.validation.constraints.NotBlank"),
                report);
    }

    @Test
    void flagsAJavaxConstraintValidatorImplementation() {
        String report = report();

        assertTrue(report.contains("JavaxReferenceValidator")
                && report.contains("javax.validation.ConstraintValidator"), report);
    }

    @Test
    void flagsACallOnAJavaxConstraintViolationException() {
        String report = report();

        assertTrue(report.contains("ViolationRethrower")
                && report.contains("javax.validation.ConstraintViolationException"), report);
    }

    @Test
    void flagsACatchClauseAlone() {
        String report = report();

        assertTrue(report.contains("ViolationSwallower.trySubmit")
                && report.contains("catches type <javax.validation.ConstraintViolationException>"), report);
    }

    @Test
    void ignoresJakartaValidation() {
        String report = report();

        assertFalse(report.contains("JakartaPayment"), report);
        assertFalse(report.contains("jakarta.validation"), report);
    }

    @Test
    void ignoresOtherJavaxPackages() {
        String report = report();

        assertFalse(report.contains("HandCheckedRefund"), report);
    }

    @Test
    void everyFixtureMatchesItsVerdict() {
        FixtureVerdicts.assertMatch(NoJavaxValidationRuleTest.class, NoJavaxValidationRule.DEFINITION, EXAMPLES);
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoJavaxValidationRule.rule, ID, EXAMPLES);

        assertTrue(debt.contains("HalfMigratedOrder"), debt);
        assertTrue(debt.contains("JavaxReferenceValidator"), debt);
        assertTrue(debt.contains("ViolationRethrower"), debt);
    }
}
