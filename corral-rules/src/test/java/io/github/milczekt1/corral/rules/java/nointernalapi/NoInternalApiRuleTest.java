package io.github.milczekt1.corral.rules.java.nointernalapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.java.nointernalapi.fixtures.CheckoutEvent;
import io.github.milczekt1.corral.rules.java.nointernalapi.fixtures.EmbeddedHttpServer;
import io.github.milczekt1.corral.rules.java.nointernalapi.fixtures.OffHeapCounter;
import io.github.milczekt1.corral.rules.java.nointernalapi.fixtures.ReloadSignalHandler;
import io.github.milczekt1.corral.rules.java.nointernalapi.fixtures.SerializationInstantiator;
import io.github.milczekt1.corral.rules.java.nointernalapi.fixtures.SolarDashboard;
import io.github.milczekt1.corral.rules.java.nointernalapi.fixtures.VarHandleCounter;
import io.github.milczekt1.corral.rules.java.nointernalapi.fixtures.sun.SolarReading;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class NoInternalApiRuleTest {

    private static final String ID = "corral.java.no-internal-api";

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

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoInternalApiRule.rule, ID, EXAMPLES);

        assertTrue(debt.contains("OffHeapCounter.unsafe"), debt);
        assertTrue(debt.contains("ReloadSignalHandler.handle"), debt);
        assertTrue(debt.contains("SerializationInstantiator.factory"), debt);
    }
}
