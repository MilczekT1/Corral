package io.github.milczekt1.corral.warn;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/**
 * The "once per run" mechanics. Every test passes its own {@link AtomicBoolean} — the process-wide one
 * would leak across the Surefire JVM.
 */
class FirstEvaluationWarningTest {

    private static ArchRule matchingNothing(String id) {
        return noClasses().should().haveNameMatching("does.not.Exist").allowEmptyShould(true).as(id);
    }

    private static ArchRule wrapped(ArchRule rule, Supplier<String> message, List<String> logged) {
        return FirstEvaluationWarning.wrap(rule, new AtomicBoolean(false), List.of(message), logged::add);
    }

    @Test
    void theWrapperLogsTheMessageOnTheFirstCheck() {
        List<String> logged = new ArrayList<>();
        ArchRule wrapped = wrapped(matchingNothing("test.no-wrapper-fixture-a"), () -> "a warning", logged);

        wrapped.check(nothingToMatch());

        assertEquals(List.of("a warning"), logged);
    }

    @Test
    void theWrapperLogsNothingWhenTheMessageSupplierReturnsNull() {
        List<String> logged = new ArrayList<>();
        ArchRule wrapped = wrapped(matchingNothing("test.no-wrapper-fixture-b"), () -> null, logged);

        wrapped.check(nothingToMatch());

        assertTrue(logged.isEmpty(), logged::toString);
    }

    @Test
    void everyNonNullMessageIsLoggedInOrderAndANullOneIsSkipped() {
        List<String> logged = new ArrayList<>();
        ArchRule wrapped = FirstEvaluationWarning.wrap(matchingNothing("test.no-wrapper-fixture-f"),
                new AtomicBoolean(false), List.of(() -> "first", () -> null, () -> "third"), logged::add);

        wrapped.check(nothingToMatch());

        assertEquals(List.of("first", "third"), logged);
    }

    /** Once per run, not once per rule: two DIFFERENT wrapped rules sharing one flag. */
    @Test
    void theWarningFiresOnceAcrossMultipleRulesSharingTheFlag() {
        List<String> logged = new ArrayList<>();
        AtomicBoolean sharedAcrossTheWholeRun = new AtomicBoolean(false);

        ArchRule firstRuleEvaluated = FirstEvaluationWarning.wrap(matchingNothing("test.no-wrapper-fixture-c"),
                sharedAcrossTheWholeRun, List.of(() -> "a warning"), logged::add);
        ArchRule secondRuleEvaluated = FirstEvaluationWarning.wrap(matchingNothing("test.no-wrapper-fixture-d"),
                sharedAcrossTheWholeRun, List.of(() -> "a warning"), logged::add);

        firstRuleEvaluated.check(nothingToMatch());
        secondRuleEvaluated.check(nothingToMatch());

        assertEquals(1, logged.size(),
                () -> "must fire once for the whole run, not once per rule: " + logged);
    }

    /** Not once per {@code check} either — a rule may be evaluated more than once in a run. */
    @Test
    void theWarningFiresOnceEvenIfTheSameRuleIsCheckedTwice() {
        List<String> logged = new ArrayList<>();
        ArchRule wrapped = wrapped(matchingNothing("test.no-wrapper-fixture-e"), () -> "a warning", logged);

        wrapped.check(nothingToMatch());
        wrapped.evaluate(nothingToMatch());

        assertEquals(1, logged.size(), () -> "must fire once per run: " + logged);
    }

    @Test
    void theWrapperDelegatesCheckToTheUnderlyingRule() {
        ArchRule alwaysViolated = noClasses().should().haveNameMatching(".*").as("test.no-wrapper-delegates-check");
        ArchRule wrapped = wrapped(alwaysViolated, () -> null, new ArrayList<>());

        JavaClasses classes = someClasses();

        assertThrows(AssertionError.class, () -> wrapped.check(classes),
                "the wrapper must not swallow the delegate's own outcome");
    }

    @Test
    void theWrapperDelegatesEvaluateToTheUnderlyingRule() {
        ArchRule alwaysViolated = noClasses().should().haveNameMatching(".*").as("test.no-wrapper-delegates-evaluate");
        ArchRule wrapped = wrapped(alwaysViolated, () -> null, new ArrayList<>());

        EvaluationResult result = wrapped.evaluate(someClasses());

        assertTrue(result.hasViolation(), "the wrapper must report the delegate's real outcome");
    }

    @Test
    void theWrapperDelegatesGetDescription() {
        ArchRule wrapped = wrapped(matchingNothing("test.no-wrapper-description"), () -> null, new ArrayList<>());

        assertEquals("test.no-wrapper-description", wrapped.getDescription());
    }

    @Test
    void asReturnsANewWrapperPreservingTheDelegate() {
        List<String> logged = new ArrayList<>();
        ArchRule wrapped = wrapped(matchingNothing("test.no-wrapper-as-original"), () -> "still warns", logged);

        ArchRule renamed = wrapped.as("test.no-wrapper-as-renamed");

        assertEquals("test.no-wrapper-as-renamed", renamed.getDescription());
        renamed.check(nothingToMatch());
        assertEquals(List.of("still warns"), logged,
                "the renamed wrapper must still carry the once-only warning behaviour");
    }

    @Test
    void becauseDelegatesToTheUnderlyingRule() {
        ArchRule wrapped = wrapped(matchingNothing("test.no-wrapper-because"), () -> null, new ArrayList<>());

        ArchRule withReason = wrapped.because("a reason");

        assertTrue(withReason.getDescription().contains("a reason"), withReason.getDescription());
        assertDoesNotThrow(() -> withReason.check(nothingToMatch()));
    }

    @Test
    void allowEmptyShouldDelegatesToTheUnderlyingRule() {
        ArchRule rule = classes().should().haveNameMatching("does.not.Exist").as("test.no-wrapper-allow-empty");
        ArchRule wrapped = wrapped(rule, () -> null, new ArrayList<>());

        ArchRule allowingEmpty = wrapped.allowEmptyShould(true);

        assertDoesNotThrow(() -> allowingEmpty.check(nothingToMatch()));
    }

    /** The production sink, asserted on the real {@link System#err}, not just an injected seam. */
    @Test
    void theRealProductionSinkWritesTheMessageToRealSystemErr() {
        ArchRule wrapped = FirstEvaluationWarning.wrap(matchingNothing("test.no-wrapper-real-stderr"),
                new AtomicBoolean(false), List.of(() -> "a first-evaluation warning probe"),
                FirstEvaluationWarning::printWarning);

        String captured = withCapturedSystemErr(() -> wrapped.check(nothingToMatch()));

        assertTrue(captured.contains("WARNING: Corral — a first-evaluation warning probe"), captured);
    }

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
        return new ClassFileImporter().importClasses(FirstEvaluationWarning.class);
    }
}
