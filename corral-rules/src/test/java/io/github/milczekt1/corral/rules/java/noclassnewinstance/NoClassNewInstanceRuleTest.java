package io.github.milczekt1.corral.rules.java.noclassnewinstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.rules.java.noclassnewinstance.fixtures.ReflectiveFactory;
import io.github.milczekt1.corral.rules.java.noclassnewinstance.fixtures.SafeInstantiation;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoClassNewInstanceRuleTest {

    private static final String ID = "corral.java.no-class-new-instance";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoClassNewInstanceRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(EXAMPLES);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("ReflectiveFactory.fromType("), debt);
            assertTrue(debt.contains("ReflectiveFactory.fromName("), debt);
            assertTrue(debt.contains("ReflectiveFactory.callableOf("), debt);
            assertTrue(debt.contains("ReflectiveFactory.lambdaOf("), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
