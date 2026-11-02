package io.github.milczekt1.corral.rules.java.noclassnewinstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.java.noclassnewinstance.fixtures.ReflectiveFactory;
import io.github.milczekt1.corral.rules.java.noclassnewinstance.fixtures.SafeInstantiation;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoClassNewInstanceRuleTest {

    private static final String ID = "corral.java.no-class-new-instance";

    private static final String CALL = "calls method <java.lang.Class.newInstance()>";

    private static final JavaClasses EXAMPLES =
            new ClassFileImporter().importClasses(ReflectiveFactory.class, SafeInstantiation.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violations() {
        return NoClassNewInstanceRule.DEFINITION.evaluate(EXAMPLES).getFailureReport().getDetails();
    }

    private static void assertFlagged(String caller, String access) {
        List<String> violations = violations();

        assertTrue(violations.stream().anyMatch(line -> line.contains(caller) && line.contains(access)),
                String.join("\n", violations));
    }

    private static void assertNotReported(String subject) {
        String report = String.join("\n", violations());

        assertFalse(report.contains(subject), report);
    }

    @Test
    void flagsACallOnAClassParameter() {
        assertFlagged("ReflectiveFactory.fromType(", CALL);
    }

    @Test
    void flagsACallOnAClassLoadedByName() {
        assertFlagged("ReflectiveFactory.fromName(", CALL);
    }

    @Test
    void flagsAMethodReference() {
        assertFlagged("ReflectiveFactory.callableOf(", "references method <java.lang.Class.newInstance()>");
    }

    @Test
    void flagsACallInsideALambda() {
        assertFlagged("ReflectiveFactory.lambdaOf(", CALL);
    }

    @Test
    void reportsExactlyFourAccesses() {
        assertEquals(4, violations().size(), String.join("\n", violations()));
    }

    @Test
    void ignoresTheConstructorReplacementBesideFlaggedCalls() {
        assertNotReported("viaConstructor");
    }

    @Test
    void ignoresArrayNewInstanceAndANewInstanceMethodOfAnotherType() {
        assertNotReported("SafeInstantiation");
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoClassNewInstanceRule.rule, ID, EXAMPLES);

        assertTrue(debt.contains("ReflectiveFactory.fromType("), debt);
        assertTrue(debt.contains("ReflectiveFactory.fromName("), debt);
        assertTrue(debt.contains("ReflectiveFactory.callableOf("), debt);
        assertTrue(debt.contains("ReflectiveFactory.lambdaOf("), debt);
    }
}
