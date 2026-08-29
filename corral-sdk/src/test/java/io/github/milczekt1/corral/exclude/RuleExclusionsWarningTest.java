package io.github.milczekt1.corral.exclude;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import io.github.milczekt1.corral.exclude.RuleExclusions.Loaded;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

/**
 * The unknown-id guardrail, as a warning. Two layers, tested separately:
 * {@link RuleExclusions#unmatchedWarning(Loaded, java.util.Collection)} decides what to say, and the
 * wrapper handles "once per run".
 *
 * <p>Every test passes its own {@link AtomicBoolean} and {@link Loaded} — the process-wide ones would
 * leak across the Surefire JVM.
 */
class RuleExclusionsWarningTest {

    @Test
    void anUnmatchedExclusionWarns() {
        Loaded state = RuleExclusions.parse(
                "test.no-unmatched-warning-fixture :: does not apply here", "x");

        String message = RuleExclusions.unmatchedWarning(state, Set.of());

        assertTrue(message.contains("test.no-unmatched-warning-fixture"), message);
        assertTrue(message.contains("matched no rule in this run"), message);
        assertTrue(message.contains(RuleExclusions.EXCLUSIONS_FILE), message);
    }

    @Test
    void aMatchedExclusionDoesNotWarn() {
        Loaded state = RuleExclusions.parse(
                "test.no-matched-warning-fixture :: does not apply here", "x");

        String message = RuleExclusions.unmatchedWarning(state, Set.of("test.no-matched-warning-fixture"));

        assertNull(message, message);
    }

    @Test
    void noExclusionsAtAllWarnsNothing() {
        assertNull(RuleExclusions.unmatchedWarning(Loaded.none(), Set.of()));
    }

    /** Every unmatched id at once, not one per run. */
    @Test
    void everyUnmatchedIdIsListedNotOnlyTheFirst() {
        Loaded state = RuleExclusions.parse("""
                test.no-first-unmatched-fixture :: one
                test.no-second-unmatched-fixture :: two
                """, "x");

        String message = RuleExclusions.unmatchedWarning(state, Set.of());

        assertTrue(message.contains("test.no-first-unmatched-fixture"), message);
        assertTrue(message.contains("test.no-second-unmatched-fixture"), message);
        assertTrue(message.contains("2 exclusions"), message);
    }

    @Test
    void aSingleUnmatchedIdIsSingular() {
        Loaded state = RuleExclusions.parse("test.no-singular-warning-fixture :: x", "x");

        String message = RuleExclusions.unmatchedWarning(state, Set.of());

        assertTrue(message.contains("1 exclusion "), message);
        assertFalse(message.contains("1 exclusions"), message);
    }

    /** {@code applyTo} is the only place a match is recorded. */
    @Test
    void applyToRecordsAMatchWhenTheRuleIsExcluded() {
        String id = "test.no-applyto-records-match-fixture";
        Loaded state = RuleExclusions.parse(id + " :: applies here", "x");
        ArchRule rule = noClasses().should().haveNameMatching("does.not.Exist").as(id);

        assertFalse(RuleExclusions.hasMatched(id),
                "must not already be recorded, or this test proves nothing");

        RuleExclusions.applyTo(rule, id, state);

        assertTrue(RuleExclusions.hasMatched(id));
    }

    @Test
    void applyToRecordsNoMatchWhenTheRuleIsNotExcluded() {
        String excludedId = "test.no-applyto-unmatched-a";
        String otherId = "test.no-applyto-unmatched-b";
        Loaded state = RuleExclusions.parse(excludedId + " :: applies here", "x");
        ArchRule rule = noClasses().should().haveNameMatching("does.not.Exist").as(otherId);

        RuleExclusions.applyTo(rule, otherId, state);

        assertFalse(RuleExclusions.hasMatched(otherId));
    }

    // -- the wrapper's "once per run, not once per rule" mechanics --------------------------------

    @Test
    void theWrapperLogsTheMessageOnTheFirstCheck() {
        List<String> logged = new ArrayList<>();
        ArchRule wrapped = RuleExclusions.warnUnmatchedExclusionsOnFirstEvaluation(
                noClasses().should().haveNameMatching("does.not.Exist").allowEmptyShould(true).as("test.no-wrapper-fixture-a"),
                new AtomicBoolean(false), () -> "an unmatched exclusion", logged::add);

        wrapped.check(nothingToMatch());

        assertEquals(List.of("an unmatched exclusion"), logged);
    }

    @Test
    void theWrapperLogsNothingWhenTheMessageSupplierReturnsNull() {
        List<String> logged = new ArrayList<>();
        ArchRule wrapped = RuleExclusions.warnUnmatchedExclusionsOnFirstEvaluation(
                noClasses().should().haveNameMatching("does.not.Exist").allowEmptyShould(true).as("test.no-wrapper-fixture-b"),
                new AtomicBoolean(false), () -> null, logged::add);

        wrapped.check(nothingToMatch());

        assertTrue(logged.isEmpty(), logged::toString);
    }

    /** Once per run, not once per rule: two DIFFERENT wrapped rules sharing one flag. */
    @Test
    void theWarningFiresOnceAcrossMultipleRulesSharingTheFlag() {
        List<String> logged = new ArrayList<>();
        AtomicBoolean sharedAcrossTheWholeRun = new AtomicBoolean(false);

        ArchRule firstRuleEvaluated = RuleExclusions.warnUnmatchedExclusionsOnFirstEvaluation(
                noClasses().should().haveNameMatching("does.not.Exist").allowEmptyShould(true).as("test.no-wrapper-fixture-c"),
                sharedAcrossTheWholeRun, () -> "an unmatched exclusion", logged::add);
        ArchRule secondRuleEvaluated = RuleExclusions.warnUnmatchedExclusionsOnFirstEvaluation(
                noClasses().should().haveNameMatching("does.not.Exist").allowEmptyShould(true).as("test.no-wrapper-fixture-d"),
                sharedAcrossTheWholeRun, () -> "an unmatched exclusion", logged::add);

        firstRuleEvaluated.check(nothingToMatch());
        secondRuleEvaluated.check(nothingToMatch());

        assertEquals(1, logged.size(),
                () -> "must fire once for the whole run, not once per rule: " + logged);
    }

    /** Not once per {@code check} either — a rule may be evaluated more than once in a run. */
    @Test
    void theWarningFiresOnceEvenIfTheSameRuleIsCheckedTwice() {
        List<String> logged = new ArrayList<>();
        ArchRule wrapped = RuleExclusions.warnUnmatchedExclusionsOnFirstEvaluation(
                noClasses().should().haveNameMatching("does.not.Exist").allowEmptyShould(true).as("test.no-wrapper-fixture-e"),
                new AtomicBoolean(false), () -> "an unmatched exclusion", logged::add);

        wrapped.check(nothingToMatch());
        wrapped.evaluate(nothingToMatch());

        assertEquals(1, logged.size(), () -> "must fire once per run: " + logged);
    }

    @Test
    void theWrapperDelegatesCheckToTheUnderlyingRule() {
        ArchRule alwaysViolated = noClasses().should().haveNameMatching(".*").as("test.no-wrapper-delegates-check");
        ArchRule wrapped = RuleExclusions.warnUnmatchedExclusionsOnFirstEvaluation(
                alwaysViolated, new AtomicBoolean(false), () -> null, message -> { });

        JavaClasses classes = someClasses();

        assertThrows(AssertionError.class, () -> wrapped.check(classes),
                "the wrapper must not swallow the delegate's own outcome");
    }

    @Test
    void theWrapperDelegatesEvaluateToTheUnderlyingRule() {
        ArchRule alwaysViolated = noClasses().should().haveNameMatching(".*").as("test.no-wrapper-delegates-evaluate");
        ArchRule wrapped = RuleExclusions.warnUnmatchedExclusionsOnFirstEvaluation(
                alwaysViolated, new AtomicBoolean(false), () -> null, message -> { });

        EvaluationResult result = wrapped.evaluate(someClasses());

        assertTrue(result.hasViolation(), "the wrapper must report the delegate's real outcome");
    }

    @Test
    void theWrapperDelegatesGetDescription() {
        ArchRule rule = noClasses().should().haveNameMatching(".*").as("test.no-wrapper-description");
        ArchRule wrapped = RuleExclusions.warnUnmatchedExclusionsOnFirstEvaluation(
                rule, new AtomicBoolean(false), () -> null, message -> { });

        assertEquals("test.no-wrapper-description", wrapped.getDescription());
    }

    /** {@code as} must keep wrapping — a renamed rule still gets the once-only warning behaviour. */
    @Test
    void asReturnsANewWrapperPreservingTheDelegate() {
        List<String> logged = new ArrayList<>();
        ArchRule rule = noClasses().should().haveNameMatching("does.not.Exist").allowEmptyShould(true).as("test.no-wrapper-as-original");
        ArchRule wrapped = RuleExclusions.warnUnmatchedExclusionsOnFirstEvaluation(
                rule, new AtomicBoolean(false), () -> "still warns", logged::add);

        ArchRule renamed = wrapped.as("test.no-wrapper-as-renamed");

        assertEquals("test.no-wrapper-as-renamed", renamed.getDescription());
        renamed.check(nothingToMatch());
        assertEquals(List.of("still warns"), logged,
                "the renamed wrapper must still carry the once-only warning behaviour");
    }

    @Test
    void becauseDelegatesToTheUnderlyingRule() {
        ArchRule rule = noClasses().should().haveNameMatching("does.not.Exist").allowEmptyShould(true).as("test.no-wrapper-because");
        ArchRule wrapped = RuleExclusions.warnUnmatchedExclusionsOnFirstEvaluation(
                rule, new AtomicBoolean(false), () -> null, message -> { });

        ArchRule withReason = wrapped.because("a reason");

        assertDoesNotThrow(() -> withReason.check(nothingToMatch()));
    }

    @Test
    void allowEmptyShouldDelegatesToTheUnderlyingRule() {
        ArchRule rule = noClasses().should().haveNameMatching("does.not.Exist").as("test.no-wrapper-allow-empty");
        ArchRule wrapped = RuleExclusions.warnUnmatchedExclusionsOnFirstEvaluation(
                rule, new AtomicBoolean(false), () -> null, message -> { });

        ArchRule allowingEmpty = wrapped.allowEmptyShould(true);

        assertDoesNotThrow(() -> allowingEmpty.check(nothingToMatch()));
    }

    @Test
    void theProductionFactoryReturnsTheSameRuleWhenNothingIsExcluded() {
        ArchRule rule = noClasses().should().haveNameMatching("does.not.Exist").allowEmptyShould(true).as("test.no-wrapper-production");

        ArchRule wrapped = RuleExclusions.warnUnmatchedExclusionsOnFirstEvaluation(rule);

        assertTrue(RuleExclusions.inEffect().isEmpty(),
                "this module's test classpath must carry no exclusions file");
        assertSame(rule, wrapped, "an unwrapped rule, not a no-op wrapper");
        assertDoesNotThrow(() -> wrapped.check(nothingToMatch()));
    }

    // -- proving the message actually reaches a stream a consumer sees, not just an injected seam --

    /**
     * Drives the wrapper with the production sink, {@link RuleExclusions#printWarning}, and asserts on
     * the real {@link System#err} — a seam-only test passes even if the sink swallows everything.
     */
    @Test
    void theRealProductionSinkWritesTheMessageToRealSystemErr() {
        ArchRule wrapped = RuleExclusions.warnUnmatchedExclusionsOnFirstEvaluation(
                noClasses().should().haveNameMatching("does.not.Exist").allowEmptyShould(true)
                        .as("test.no-wrapper-real-stderr"),
                new AtomicBoolean(false), () -> "corral-exclusions.txt: an unmatched exclusion probe",
                RuleExclusions::printWarning);

        String captured = withCapturedSystemErr(() -> wrapped.check(nothingToMatch()));

        assertTrue(captured.contains("corral-exclusions.txt: an unmatched exclusion probe"), captured);
    }

    /** The unit underneath the test above: {@code printWarning} itself really writes to stderr. */
    @Test
    void printWarningWritesToRealSystemErr() {
        String captured = withCapturedSystemErr(() -> RuleExclusions.printWarning("a direct probe message"));

        assertTrue(captured.contains("a direct probe message"), captured);
    }

    /** Redirects {@link System#err} for the duration of {@code action} and returns what it captured. */
    private static String withCapturedSystemErr(Runnable action) {
        PrintStream original = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try (PrintStream redirected = new PrintStream(captured, true, StandardCharsets.UTF_8)) {
            System.setErr(redirected);
            action.run();
        } finally {
            System.setErr(original);
        }
        return captured.toString(StandardCharsets.UTF_8);
    }

    private static JavaClasses nothingToMatch() {
        return new ClassFileImporter().importClasses();
    }

    private static JavaClasses someClasses() {
        return new ClassFileImporter().importClasses(RuleExclusions.class, Exclusion.class);
    }
}
