package io.github.milczekt1.corral.rules.jakarta.nojavaxpersistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.rules.jakarta.nojavaxpersistence.fixtures.HalfMigratedInvoice;
import io.github.milczekt1.corral.rules.jakarta.nojavaxpersistence.fixtures.JakartaShipment;
import io.github.milczekt1.corral.rules.jakarta.nojavaxpersistence.fixtures.JavaxMoneyConverter;
import io.github.milczekt1.corral.rules.jakarta.nojavaxpersistence.fixtures.LegacyOrderLookup;
import io.github.milczekt1.corral.rules.jakarta.nojavaxpersistence.fixtures.OptimisticLockSwallower;
import io.github.milczekt1.corral.rules.jakarta.nojavaxpersistence.fixtures.PersistenceSupport;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The examples live in {@code fixtures/}, which Surefire and Sonar both exclude: they are
 * deliberately bad code, and a linter told to fix any of it would delete the violation under test.
 */
class NoJavaxPersistenceRuleTest {

    private static final String ID = "corral.jakarta.no-javax-persistence";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    private static final JavaClasses EXAMPLES = new ClassFileImporter().importClasses(
            HalfMigratedInvoice.class, JavaxMoneyConverter.class, LegacyOrderLookup.class,
            OptimisticLockSwallower.class, JakartaShipment.class, PersistenceSupport.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violations() {
        return NoJavaxPersistenceRule.DEFINITION.evaluate(EXAMPLES).getFailureReport().getDetails();
    }

    private static void assertFlagged(String subject, String target) {
        List<String> violations = violations();

        assertTrue(violations.stream().anyMatch(line -> line.contains(subject) && line.contains(target)),
                String.join("\n", violations));
    }

    @Test
    void flagsAJavaxMappingAnnotationOnAJakartaEntity() {
        assertFlagged("HalfMigratedInvoice.number", "<javax.persistence.Column>");
    }

    @Test
    void flagsAJavaxAttributeConverterImplementation() {
        assertFlagged("JavaxMoneyConverter", "<javax.persistence.AttributeConverter>");
    }

    @Test
    void flagsAnEntityManagerField() {
        assertFlagged("LegacyOrderLookup.entityManager", "<javax.persistence.EntityManager>");
    }

    @Test
    void flagsAJavaxQueryReturnType() {
        assertFlagged("LegacyOrderLookup.byCustomer", "return type <javax.persistence.Query>");
    }

    @Test
    void doesNotSeeACatchClauseAlone() {
        String report = String.join("\n", violations());

        assertFalse(report.contains("OptimisticLockSwallower"), report);
    }

    @Test
    void ignoresJakartaPersistence() {
        String report = String.join("\n", violations());

        assertFalse(report.contains("JakartaShipment"), report);
        assertFalse(report.contains("jakarta.persistence"), report);
    }

    @Test
    void ignoresOtherJavaxPackages() {
        String report = String.join("\n", violations());

        assertFalse(report.contains("PersistenceSupport"), report);
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoJavaxPersistenceRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(EXAMPLES);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("HalfMigratedInvoice"), debt);
            assertTrue(debt.contains("JavaxMoneyConverter"), debt);
            assertTrue(debt.contains("LegacyOrderLookup"), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
