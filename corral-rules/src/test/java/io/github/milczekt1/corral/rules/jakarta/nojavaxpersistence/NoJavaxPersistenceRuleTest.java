package io.github.milczekt1.corral.rules.jakarta.nojavaxpersistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.jakarta.nojavaxpersistence.fixtures.HalfMigratedInvoice;
import io.github.milczekt1.corral.rules.jakarta.nojavaxpersistence.fixtures.JakartaShipment;
import io.github.milczekt1.corral.rules.jakarta.nojavaxpersistence.fixtures.JavaxMoneyConverter;
import io.github.milczekt1.corral.rules.jakarta.nojavaxpersistence.fixtures.LegacyOrderLookup;
import io.github.milczekt1.corral.rules.jakarta.nojavaxpersistence.fixtures.OptimisticLockSwallower;
import io.github.milczekt1.corral.rules.jakarta.nojavaxpersistence.fixtures.PersistenceSupport;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import io.github.milczekt1.corral.rules.testsupport.FixtureVerdicts;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The examples live in {@code fixtures/}, which Surefire and Sonar both exclude: they are
 * deliberately bad code, and a linter told to fix any of it would delete the violation under test.
 */
class NoJavaxPersistenceRuleTest {

    private static final String ID = "corral.jakarta.no-javax-persistence";

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
    void flagsACatchClauseAlone() {
        assertFlagged("OptimisticLockSwallower.trySave",
                "catches type <javax.persistence.OptimisticLockException>");
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

    @Test
    void everyFixtureMatchesItsVerdict() {
        FixtureVerdicts.assertMatch(NoJavaxPersistenceRuleTest.class, NoJavaxPersistenceRule.DEFINITION, EXAMPLES);
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoJavaxPersistenceRule.rule, ID, EXAMPLES);

        assertTrue(debt.contains("HalfMigratedInvoice"), debt);
        assertTrue(debt.contains("JavaxMoneyConverter"), debt);
        assertTrue(debt.contains("LegacyOrderLookup"), debt);
    }
}
