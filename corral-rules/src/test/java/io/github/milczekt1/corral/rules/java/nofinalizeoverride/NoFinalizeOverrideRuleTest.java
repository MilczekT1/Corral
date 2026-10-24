package io.github.milczekt1.corral.rules.java.nofinalizeoverride;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.rules.java.nofinalizeoverride.fixtures.CleanerBackedBuffer;
import io.github.milczekt1.corral.rules.java.nofinalizeoverride.fixtures.FinalizeContract;
import io.github.milczekt1.corral.rules.java.nofinalizeoverride.fixtures.HardenedBase;
import io.github.milczekt1.corral.rules.java.nofinalizeoverride.fixtures.LeakyConnection;
import io.github.milczekt1.corral.rules.java.nofinalizeoverride.fixtures.PublicFinalizer;
import io.github.milczekt1.corral.rules.java.nofinalizeoverride.fixtures.SafeConnection;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoFinalizeOverrideRuleTest {

    private static final String ID = "corral.java.no-finalize-override";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    private static final JavaClasses EXAMPLES = new ClassFileImporter().importClasses(
            LeakyConnection.class, HardenedBase.class, PublicFinalizer.class, FinalizeContract.class,
            SafeConnection.class, CleanerBackedBuffer.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violations() {
        return NoFinalizeOverrideRule.DEFINITION.evaluate(EXAMPLES).getFailureReport().getDetails();
    }

    private static void assertFlagged(String method) {
        List<String> violations = violations();

        assertTrue(violations.stream().anyMatch(line -> line.contains(method)), String.join("\n", violations));
    }

    private static void assertNotReported(String subject) {
        String report = String.join("\n", violations());

        assertFalse(report.contains(subject), report);
    }

    @Test
    void flagsAProtectedOverrideThatThrowsThrowable() {
        assertFlagged("LeakyConnection.finalize()>");
    }

    @Test
    void flagsAnEmptyFinalFinalizer() {
        assertFlagged("HardenedBase.finalize()>");
    }

    @Test
    void flagsAnOverrideWidenedToPublic() {
        assertFlagged("PublicFinalizer.finalize()>");
    }

    @Test
    void flagsAnInterfaceDeclaringFinalize() {
        assertFlagged("FinalizeContract.finalize()>");
    }

    @Test
    void reportsExactlyFourMethods() {
        assertEquals(4, violations().size(), String.join("\n", violations()));
    }

    @Test
    void ignoresAFinalizeOverloadWithAParameterBesideAFlaggedOverride() {
        assertNotReported("LeakyConnection.finalize(java.lang.String)");
    }

    @Test
    void ignoresANoArgCloseBesideAFlaggedOverride() {
        assertNotReported("LeakyConnection.close()");
    }

    @Test
    void ignoresAutoCloseableWithADomainFinalizeMethod() {
        assertNotReported("SafeConnection");
    }

    @Test
    void ignoresACleanerBackstop() {
        assertNotReported("CleanerBackedBuffer");
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoFinalizeOverrideRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(EXAMPLES);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("LeakyConnection.finalize()>"), debt);
            assertTrue(debt.contains("HardenedBase.finalize()>"), debt);
            assertTrue(debt.contains("PublicFinalizer.finalize()>"), debt);
            assertTrue(debt.contains("FinalizeContract.finalize()>"), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
