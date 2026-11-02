package io.github.milczekt1.corral.rules.shedlock.nolocknameover64chars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.shedlock.nolocknameover64chars.fixtures.CdiInvoiceJobs;
import io.github.milczekt1.corral.rules.shedlock.nolocknameover64chars.fixtures.DunningRunService;
import io.github.milczekt1.corral.rules.shedlock.nolocknameover64chars.fixtures.LegacyInvoiceJobs;
import io.github.milczekt1.corral.rules.shedlock.nolocknameover64chars.fixtures.LookalikeLockJobs;
import io.github.milczekt1.corral.rules.shedlock.nolocknameover64chars.fixtures.MicronautDunningService;
import io.github.milczekt1.corral.rules.shedlock.nolocknameover64chars.fixtures.MicronautInvoiceJobs;
import io.github.milczekt1.corral.rules.shedlock.nolocknameover64chars.fixtures.OverdueRemindersLock;
import io.github.milczekt1.corral.rules.shedlock.nolocknameover64chars.fixtures.SpringInvoiceJobs;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import java.io.IOException;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NoLockNameOver64CharsRuleTest {

    private static final String ID = "corral.shedlock.no-lock-name-over-64-chars";

    private static final JavaClasses EXAMPLES = new ClassFileImporter().importClasses(
            SpringInvoiceJobs.class, OverdueRemindersLock.class, LegacyInvoiceJobs.class, CdiInvoiceJobs.class,
            MicronautInvoiceJobs.class, MicronautDunningService.class, DunningRunService.class,
            LookalikeLockJobs.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violations() {
        return NoLockNameOver64CharsRule.DEFINITION.evaluate(EXAMPLES).getFailureReport().getDetails();
    }

    private static void assertFlagged(String member) {
        List<String> violations = violations();

        assertTrue(violations.stream().anyMatch(line -> line.contains("fixtures." + member)),
                String.join("\n", violations));
    }

    private static void assertNotReported(String member) {
        String report = String.join("\n", violations());

        assertFalse(report.contains("fixtures." + member), report);
    }

    private static String springLockName(String method) throws NoSuchMethodException {
        return SpringInvoiceJobs.class.getMethod(method).getAnnotation(SchedulerLock.class).name();
    }

    @Test
    void boundaryExamplesSitEitherSideOfTheLimit() throws NoSuchMethodException {
        assertEquals(64, springLockName("sendPaymentConfirmations").length());
        assertEquals(65, springLockName("escalateUnpaidInvoices").length());

        String supplementary = springLockName("notifyCollectionsTeam");
        assertEquals(64, supplementary.codePointCount(0, supplementary.length()));
        assertTrue(supplementary.length() > 64, "premise: more UTF-16 units than characters");
    }

    @Test
    void flagsALiteralNameOverTheLimit() {
        assertFlagged("SpringInvoiceJobs.sendOverdueReminders()>");
    }

    @Test
    void flagsANameHeldInAConstant() {
        assertFlagged("SpringInvoiceJobs.closeStaleDunningRuns()>");
    }

    @Test
    void flagsANameOneCharacterOverTheLimit() {
        assertFlagged("SpringInvoiceJobs.escalateUnpaidInvoices()>");
    }

    @ParameterizedTest(name = "flags {0}")
    @ValueSource(strings = {"SpringInvoiceJobs", "LegacyInvoiceJobs", "CdiInvoiceJobs", "MicronautInvoiceJobs"})
    void flagsEveryShedLockAnnotationType(String jobs) {
        assertFlagged(jobs + ".sendOverdueReminders()>");
    }

    @Test
    void flagsAnAnnotationOnAClass() {
        assertFlagged("MicronautDunningService>");
    }

    @Test
    void ignoresACdiAnnotationOnAClass() {
        assertNotReported("DunningRunService");
    }

    @Test
    void flagsTheTextOutsideAPlaceholder() {
        assertFlagged("SpringInvoiceJobs.sendRemindersUnderAppPrefix()>");
    }

    @Test
    void flagsACdiPlaceholderAsWritten() {
        assertFlagged("CdiInvoiceJobs.closeStaleDunningRuns()>");
    }

    @Test
    void flagsAComposedAnnotationType() {
        assertFlagged("OverdueRemindersLock>");
    }

    @Test
    void ignoresANameAtTheLimit() {
        assertNotReported("SpringInvoiceJobs.sendPaymentConfirmations()");
    }

    @Test
    void countsCharactersNotUtf16Units() {
        assertNotReported("SpringInvoiceJobs.notifyCollectionsTeam()");
    }

    @Test
    void ignoresAPlaceholder() {
        assertNotReported("SpringInvoiceJobs.remindersFromConfig()");
    }

    @Test
    void ignoresAPlaceholderNestedInsideAnother() {
        assertNotReported("SpringInvoiceJobs.remindersWithNestedDefault()");
    }

    @Test
    void ignoresASpelTemplate() {
        assertNotReported("SpringInvoiceJobs.remindersForTenant(");
    }

    @Test
    void ignoresALongLockAtMostFor() {
        assertNotReported("SpringInvoiceJobs.purgeExpiredInvoices()");
    }

    @Test
    void ignoresALongCronBesideAShortName() {
        assertNotReported("SpringInvoiceJobs.archiveSettledInvoices()");
    }

    @Test
    void ignoresAMethodUsingTheComposedAnnotation() {
        assertNotReported("SpringInvoiceJobs.sendRemindersThroughComposedLock()");
    }

    @Test
    void ignoresAnAnnotationThatOnlySharesTheSimpleName() {
        assertNotReported("LookalikeLockJobs");
    }

    @Test
    void reportsExactlyTenDeclarations() {
        assertEquals(10, violations().size(), String.join("\n", violations()));
    }

    @Test
    void printsTheMethodLine() {
        String location = EXAMPLES.get(SpringInvoiceJobs.class).getMethod("sendOverdueReminders")
                .getSourceCodeLocation().toString();

        assertTrue(violations().contains("Method <" + SpringInvoiceJobs.class.getName()
                        + ".sendOverdueReminders()> has a @SchedulerLock name longer than 64 characters in "
                        + location),
                String.join("\n", violations()));
    }

    @Test
    void printsTheClassLine() {
        assertTrue(violations().contains("Class <" + OverdueRemindersLock.class.getName()
                        + "> has a @SchedulerLock name longer than 64 characters in (OverdueRemindersLock.java:0)"),
                String.join("\n", violations()));
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoLockNameOver64CharsRule.rule, ID, EXAMPLES);

        assertTrue(debt.contains("SpringInvoiceJobs.sendOverdueReminders()>"), debt);
        assertTrue(debt.contains("OverdueRemindersLock>"), debt);
        assertFalse(debt.contains("sendPaymentConfirmations"),
                "a name at the limit must never reach the store: " + debt);
    }
}
