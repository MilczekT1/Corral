package io.github.milczekt1.corral.exclude;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import io.github.milczekt1.corral.format.AgentFriendlyFailureDisplayFormat;
import io.github.milczekt1.corral.exclude.RuleExclusions.Loaded;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.doc.RuleRegistry;
import io.github.milczekt1.corral.fixtures.tree.FixtureRootGroup;
import io.github.milczekt1.corral.reflect.PublishedRules;
import org.junit.jupiter.api.Test;

/**
 * The unknown-id guardrail, moved to where it is sound.
 *
 * <p>It cannot live in {@code guard()}: at the moment one rule is evaluated, {@code RuleRegistry}
 * holds only the rules loaded so far, so a run that wires one group would call every other group's
 * exclusions typos. Walking a wired root is different — it forces every reachable rule class to
 * initialise, so the id set it yields is complete by construction for the rules that root publishes.
 */
class RuleExclusionsResolveTest {

    /** {@code FixtureRootGroup} publishes {@code test.no-alpha-fixture} and {@code test.no-beta-fixture}. */
    private static final Class<?> ROOT = FixtureRootGroup.class;

    @Test
    void anIdReachableFromTheWiredRootResolves() {
        ArchRule check = RuleExclusions.resolvedAgainst(ROOT,
                RuleExclusions.parse("test.no-alpha-fixture :: does not apply here", "x"));

        assertDoesNotThrow(() -> check.check(nothingToMatch()));
    }

    /**
     * The failure mode the guardrail exists for: a typo, or a rule renamed upstream. Either way the
     * line removes nothing while reading in the diff as though it did.
     */
    @Test
    void anIdReachableFromNowhereFailsAndListsWhatIsAvailable() {
        ArchRule check = RuleExclusions.resolvedAgainst(ROOT,
                RuleExclusions.parse("test.no-alfa-fixture :: a typo for test.no-alpha-fixture", "x"));

        JavaClasses none = nothingToMatch();

        String message = assertThrows(AssertionError.class, () -> check.check(none)).getMessage();

        assertTrue(message.contains("test.no-alfa-fixture"), message);
        assertTrue(message.contains("test.no-alpha-fixture"),
                () -> "must list the ids it could have meant: " + message);
    }

    /** Every unresolved id at once — fixing the file one round trip per line is how it gets deleted. */
    @Test
    void everyUnresolvedIdIsReportedNotOnlyTheFirst() {
        ArchRule check = RuleExclusions.resolvedAgainst(ROOT, RuleExclusions.parse("""
                test.no-alfa-fixture :: one typo
                test.no-bta-fixture :: another typo
                """, "x"));

        JavaClasses none = nothingToMatch();

        String message = assertThrows(AssertionError.class, () -> check.check(none)).getMessage();

        assertTrue(message.contains("test.no-alfa-fixture"), message);
        assertTrue(message.contains("test.no-bta-fixture"), message);
    }

    /**
     * A consumer excluding one of their OWN rules names an id no catalog root publishes. The
     * registry covers them: by the time this check runs their rule class has registered, because
     * their wiring evaluated it.
     */
    /**
     * The check must not depend on what else happened to load in this JVM. Unioning the process-wide
     * registry in made it run-dependent: an id registered by a rule class that a partial run never
     * loads was reported as a typo, failing a build that a full run passed. Walking the root is
     * deterministic, so the verdict is the same in every run.
     */
    @Test
    void theVerdictDoesNotDependOnWhatElseIsRegisteredInThisJvm() {
        String registeredButNotPublished = "test.no-resolve-registered-elsewhere-fixture";
        RuleRegistry.register(RuleDoc.builder()
                .id(registeredButNotPublished)
                .why("Registered by some other class that happened to load first.")
                .howToFix("N/A — this is a test fixture, not a real rule.")
                .build());
        assertFalse(PublishedRules.idsOf(ROOT).contains(registeredButNotPublished),
                "the root must NOT publish this id, or the test proves nothing");

        ArchRule check = RuleExclusions.resolvedAgainst(ROOT,
                RuleExclusions.parse(registeredButNotPublished + " :: my own rule", "x"));
        JavaClasses none = nothingToMatch();

        assertThrows(AssertionError.class, () -> check.check(none),
                "an id the wired root does not publish must fail identically whether or not some"
                        + " other class in this JVM registered it");
    }

    /** A consumer who names their own rule needs to be told the actual remedy, not just "unknown". */
    @Test
    void theFailureSaysWhatToDoAboutARuleTheConsumerOwns() {
        ArchRule check = RuleExclusions.resolvedAgainst(ROOT,
                RuleExclusions.parse("fixture.mine :: my own rule", "x"));
        JavaClasses none = nothingToMatch();

        String message = assertThrows(AssertionError.class, () -> check.check(none)).getMessage();

        assertTrue(message.contains("stop wiring"),
                () -> "a rule you own is removed by not wiring it, not by excluding it: " + message);
    }

    /**
     * The guard cannot be turned off by the file it guards. It is published from the same root it
     * walks, so without this it would look like a perfectly resolvable id.
     */
    @Test
    void theResolveCheckItselfCannotBeExcluded() {
        Loaded loaded = RuleExclusions.parse(
                "corral.exclusions-must-name-real-rules :: we do not want this guard", "x");

        assertNotNull(loaded.problem(), "excluding the guard must not parse as an ordinary line");
        assertTrue(loaded.problem().contains("corral.exclusions-must-name-real-rules"), loaded::problem);
        assertTrue(loaded.entries().isEmpty(),
                () -> "it must never reach the census, which would print it as not enforced: " + loaded);
    }

    /**
     * The doc exists to be read. Thrown as a bare {@link AssertionError} it never reaches
     * {@code AgentFriendlyFailureDisplayFormat}, and registering it would only be satisfying the
     * completeness test rather than telling a consumer anything.
     */
    @Test
    void theFailureRendersTheRulesOwnGuidance() {
        RuleRegistry.register(RuleExclusions.resolveDoc());
        ArchConfiguration.get().setProperty(
                "failureDisplayFormat", AgentFriendlyFailureDisplayFormat.class.getName());
        try {
            ArchRule check = RuleExclusions.resolvedAgainst(ROOT,
                    RuleExclusions.parse("test.no-alfa-fixture :: a typo", "x"));
            JavaClasses none = nothingToMatch();

            String message = assertThrows(AssertionError.class, () -> check.check(none)).getMessage();

            assertTrue(message.contains("WHY:"), () -> "no WHY section:\n" + message);
            assertTrue(message.contains("HOW TO FIX:"), () -> "no HOW TO FIX section:\n" + message);
            assertTrue(message.contains("test.no-alfa-fixture"), message);
        } finally {
            ArchConfiguration.get().reset();
        }
    }

    /**
     * What makes the rendering above possible, asserted without depending on the consumer having
     * configured a formatter: the failure travels as a violation on an {@link EvaluationResult}, not
     * as a thrown {@code AssertionError} that no formatter ever sees.
     */
    @Test
    void theFailureTravelsAsAViolationRatherThanAThrownError() {
        ArchRule check = RuleExclusions.resolvedAgainst(ROOT,
                RuleExclusions.parse("test.no-alfa-fixture :: a typo", "x"));

        EvaluationResult result = check.evaluate(nothingToMatch());

        assertTrue(result.hasViolation(), "nothing for a failure format to render");
        assertTrue(result.getFailureReport().getDetails().stream()
                        .anyMatch(line -> line.contains("test.no-alfa-fixture")),
                () -> "violation lines: " + result.getFailureReport().getDetails());
    }

    @Test
    void noExclusionsAtAllPassesWithoutWalkingAnything() {
        ArchRule check = RuleExclusions.resolvedAgainst(ROOT, Loaded.none());

        assertDoesNotThrow(() -> check.check(nothingToMatch()));
    }

    /** A file that could not be parsed is already failing every rule; this must not double-report. */
    @Test
    void abrokenFilePassesHereBecauseEveryRuleIsAlreadyFailing() {
        ArchRule check = RuleExclusions.resolvedAgainst(ROOT,
                RuleExclusions.parse("test.no-alpha-fixture", "x"));

        assertDoesNotThrow(() -> check.check(nothingToMatch()));
    }

    private static JavaClasses nothingToMatch() {
        return new ClassFileImporter().importClasses();
    }
}
