package io.github.milczekt1.corral.rules.jakarta.nojavaxvalidation;

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
import javax.naming.InvalidNameException;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintViolationException;
import javax.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;

class NoJavaxValidationRuleTest {

    /** Half-migrated on purpose: the {@code jakarta} field must not be reported beside the {@code javax} one. */
    record HalfMigratedOrder(@NotBlank String reference,
                             @jakarta.validation.constraints.NotNull Integer quantity) {
    }

    static class JavaxReferenceValidator implements ConstraintValidator<NotBlank, String> {

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value != null && !value.isBlank();
        }
    }

    static class ViolationRethrower {

        void submit(Runnable submission) {
            try {
                submission.run();
            } catch (ConstraintViolationException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }
    }

    /** ArchUnit records no dependency for a catch clause, so this is invisible and the rule's docs say so. */
    static class ViolationSwallower {

        boolean trySubmit(Runnable submission) {
            try {
                submission.run();
                return true;
            } catch (ConstraintViolationException e) {
                return false;
            }
        }
    }

    record JakartaPayment(@jakarta.validation.constraints.NotBlank String reference) {
    }

    /** {@code javax.naming} is a JDK {@code javax} package, so a predicate widened to {@code javax..} fails. */
    static class HandCheckedRefund {

        void validate(String reference) throws InvalidNameException {
            if (reference == null || reference.isBlank()) {
                throw new InvalidNameException("reference is required");
            }
        }
    }

    private static final String ID = "corral.jakarta.no-javax-validation";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

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
    void doesNotSeeACatchClauseAlone() {
        String report = report();

        assertFalse(report.contains("ViolationSwallower"), report);
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoJavaxValidationRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(EXAMPLES);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("HalfMigratedOrder"), debt);
            assertTrue(debt.contains("JavaxReferenceValidator"), debt);
            assertTrue(debt.contains("ViolationRethrower"), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
