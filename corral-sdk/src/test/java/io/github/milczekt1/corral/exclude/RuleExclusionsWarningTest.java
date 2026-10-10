package io.github.milczekt1.corral.exclude;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.exclude.RuleExclusions.Loaded;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The unknown-id guardrail, as a warning: what it says. {@code FirstEvaluationWarningTest} owns "once
 * per run".
 *
 * <p>Every test passes its own {@link Loaded} — the process-wide one would leak across the Surefire JVM.
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

    @Test
    void theProductionFactoryReturnsTheSameRuleWhenNothingIsExcluded() {
        ArchRule rule = noClasses().should().haveNameMatching("does.not.Exist").allowEmptyShould(true).as("test.no-wrapper-production");

        ArchRule wrapped = RuleExclusions.warnUnmatchedExclusionsOnFirstEvaluation(rule);

        assertTrue(RuleExclusions.inEffect().isEmpty(),
                "this module's test classpath must carry no exclusions file");
        assertSame(rule, wrapped, "an unwrapped rule, not a no-op wrapper");
        assertDoesNotThrow(() -> wrapped.check(nothingToMatch()));
    }

    private static JavaClasses nothingToMatch() {
        return new ClassFileImporter().importClasses();
    }
}
