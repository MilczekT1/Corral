package io.github.milczekt1.corral.rules.java.nolegacydateapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.rules.java.nolegacydateapi.fixtures.BillingCycle;
import io.github.milczekt1.corral.rules.java.nolegacydateapi.fixtures.DeliverySchedule;
import io.github.milczekt1.corral.rules.java.nolegacydateapi.fixtures.InvoiceRenderer;
import io.github.milczekt1.corral.rules.java.nolegacydateapi.fixtures.LedgerExporter;
import io.github.milczekt1.corral.rules.java.nolegacydateapi.fixtures.MonthNames;
import io.github.milczekt1.corral.rules.java.nolegacydateapi.fixtures.OrderRowMapper;
import io.github.milczekt1.corral.rules.java.nolegacydateapi.fixtures.PayrollRecord;
import io.github.milczekt1.corral.rules.java.nolegacydateapi.fixtures.RetryBackoff;
import io.github.milczekt1.corral.rules.java.nolegacydateapi.fixtures.ShipmentRowMapper;
import io.github.milczekt1.corral.rules.java.nolegacydateapi.fixtures.ZoneDefaults;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoLegacyDateApiRuleTest {

    private static final String ID = "corral.java.no-legacy-date-api";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    private static final JavaClasses EXAMPLES = new ClassFileImporter().importClasses(
            InvoiceRenderer.class, OrderRowMapper.class, PayrollRecord.class, BillingCycle.class,
            LedgerExporter.class, ZoneDefaults.class, DeliverySchedule.class, ShipmentRowMapper.class,
            RetryBackoff.class, MonthNames.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violations() {
        return NoLegacyDateApiRule.DEFINITION.evaluate(EXAMPLES).getFailureReport().getDetails();
    }

    private static void assertFlagged(String subject, String target) {
        List<String> violations = violations();

        assertTrue(violations.stream().anyMatch(line -> line.contains(subject) && line.contains(target)),
                String.join("\n", violations));
    }

    private static void assertNotReported(String subject) {
        String report = String.join("\n", violations());

        assertFalse(report.contains(subject), report);
    }

    @Test
    void flagsAJavaUtilDateField() {
        assertFlagged("InvoiceRenderer.issuedAt", "<java.util.Date>");
    }

    @Test
    void flagsASimpleDateFormatAsADateFormatSubtype() {
        assertFlagged("InvoiceRenderer.render", "java.text.SimpleDateFormat.<init>");
    }

    @Test
    void flagsATimestampAsADateSubtype() {
        assertFlagged("OrderRowMapper.createdAt", "java.sql.Timestamp.toInstant()");
    }

    @Test
    void flagsAJavaSqlDateField() {
        assertFlagged("PayrollRecord.paidOn", "<java.sql.Date>");
    }

    @Test
    void flagsACalendarReturnType() {
        assertFlagged("BillingCycle.nextRun", "return type <java.util.Calendar>");
    }

    @Test
    void flagsADateFormatParameter() {
        assertFlagged("LedgerExporter.export", "parameter of type <java.text.DateFormat>");
    }

    @Test
    void flagsATimeZoneLookup() {
        assertFlagged("ZoneDefaults.utcIds", "java.util.TimeZone.getTimeZone(java.lang.String)");
    }

    @Test
    void reportsOnlyTheLegacyDependencies() {
        List<String> violations = violations();

        assertEquals(12, violations.size(), String.join("\n", violations));
    }

    @Test
    void ignoresJavaTimeBesideAFlaggedField() {
        assertNotReported("java.time");
    }

    @Test
    void ignoresTheRestOfAFlaggedRowMapper() {
        assertNotReported("getString");
    }

    @Test
    void ignoresJavaTimeThroughout() {
        assertNotReported("DeliverySchedule");
    }

    @Test
    void ignoresATimestampColumnReadAsLocalDateTime() {
        assertNotReported("ShipmentRowMapper");
    }

    @Test
    void ignoresTimeUnit() {
        assertNotReported("RetryBackoff");
    }

    @Test
    void ignoresDateFormatSymbols() {
        assertNotReported("MonthNames");
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoLegacyDateApiRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(EXAMPLES);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("InvoiceRenderer.issuedAt"), debt);
            assertTrue(debt.contains("OrderRowMapper.createdAt"), debt);
            assertTrue(debt.contains("ZoneDefaults.utcIds"), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
