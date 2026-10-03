package io.github.milczekt1.corral.rules.java.nointernalapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.rules.java.nointernalapi.fixtures.CheckoutEvent;
import io.github.milczekt1.corral.rules.java.nointernalapi.fixtures.EmbeddedHttpServer;
import io.github.milczekt1.corral.rules.java.nointernalapi.fixtures.OffHeapCounter;
import io.github.milczekt1.corral.rules.java.nointernalapi.fixtures.ReloadSignalHandler;
import io.github.milczekt1.corral.rules.java.nointernalapi.fixtures.SerializationInstantiator;
import io.github.milczekt1.corral.rules.java.nointernalapi.fixtures.SolarDashboard;
import io.github.milczekt1.corral.rules.java.nointernalapi.fixtures.VarHandleCounter;
import io.github.milczekt1.corral.rules.java.nointernalapi.fixtures.sun.SolarReading;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class NoInternalApiRuleTest {

    private static final String ID = "corral.java.no-internal-api";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    private static final JavaClasses EXAMPLES = new ClassFileImporter().importClasses(
            OffHeapCounter.class, ReloadSignalHandler.class, SerializationInstantiator.class,
            VarHandleCounter.class, EmbeddedHttpServer.class, CheckoutEvent.class, SolarDashboard.class,
            SolarReading.class);

    /**
     * No fixture can reference {@code jdk.internal..} under {@code --release 17}, so a real JDK class
     * that does stands in. Kept out of {@link #EXAMPLES}: its findings vary by JDK and must not reach
     * the committed store.
     */
    private static final JavaClasses JDK_CLASS = new ClassFileImporter().importClasses(AtomicInteger.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violationsIn(JavaClasses classes) {
        return NoInternalApiRule.DEFINITION.evaluate(classes).getFailureReport().getDetails();
    }

    private static void assertFlagged(String subject, String target) {
        List<String> violations = violationsIn(EXAMPLES);

        assertTrue(violations.stream().anyMatch(line -> line.contains(subject) && line.contains(target)),
                String.join("\n", violations));
    }

    private static void assertNotReported(String subject) {
        String report = String.join("\n", violationsIn(EXAMPLES));

        assertFalse(report.contains(subject), report);
    }

    @Test
    void flagsAnUnsafeField() {
        assertFlagged("OffHeapCounter.unsafe", "<sun.misc.Unsafe>");
    }

    @Test
    void flagsASignalParameter() {
        assertFlagged("ReloadSignalHandler.handle", "parameter of type <sun.misc.Signal>");
    }

    @Test
    void flagsACallIntoSunReflect() {
        assertFlagged("SerializationInstantiator.factory", "sun.reflect.ReflectionFactory.getReflectionFactory()");
    }

    @Test
    void flagsAJdkInternalDependency() {
        List<String> violations = violationsIn(JDK_CLASS);

        assertTrue(violations.stream().anyMatch(line -> line.contains("<jdk.internal.misc.Unsafe>")),
                String.join("\n", violations));
    }

    @Test
    void reportsOnlyTheInternalDependencies() {
        List<String> violations = violationsIn(EXAMPLES);

        assertEquals(4, violations.size(), String.join("\n", violations));
    }

    @Test
    void ignoresSupportedApiBesideAFlaggedField() {
        assertNotReported("AtomicLong");
    }

    @Test
    void ignoresVarHandles() {
        assertNotReported("VarHandleCounter");
    }

    @Test
    void ignoresExportedComSunApi() {
        assertNotReported("EmbeddedHttpServer");
    }

    @Test
    void ignoresJdkPackagesOutsideJdkInternal() {
        assertNotReported("CheckoutEvent");
    }

    @Test
    void ignoresASunSegmentBelowTheRootPackage() {
        assertNotReported("SolarDashboard");
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoInternalApiRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(EXAMPLES);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("OffHeapCounter.unsafe"), debt);
            assertTrue(debt.contains("ReloadSignalHandler.handle"), debt);
            assertTrue(debt.contains("SerializationInstantiator.factory"), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
