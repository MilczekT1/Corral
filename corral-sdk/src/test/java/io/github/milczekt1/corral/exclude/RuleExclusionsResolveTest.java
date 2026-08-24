package io.github.milczekt1.corral.exclude;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.exclude.RuleExclusions.Loaded;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.doc.RuleRegistry;
import io.github.milczekt1.corral.fixtures.tree.FixtureRootGroup;
import io.github.milczekt1.corral.reflect.PublishedRules;
import org.junit.jupiter.api.BeforeAll;
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

    /** {@code FixtureRootGroup} publishes {@code fixture.alpha} and {@code fixture.beta}. */
    private static final Class<?> ROOT = FixtureRootGroup.class;

    private static final String CONSUMER_OWNED_ID = "fixture.resolve-consumer-owned";

    @Test
    void anIdReachableFromTheWiredRootResolves() {
        ArchRule check = RuleExclusions.resolvedAgainst(ROOT,
                RuleExclusions.parse("fixture.alpha :: does not apply here", "x"));

        assertDoesNotThrow(() -> check.check(nothingToMatch()));
    }

    /**
     * The failure mode the guardrail exists for: a typo, or a rule renamed upstream. Either way the
     * line removes nothing while reading in the diff as though it did.
     */
    @Test
    void anIdReachableFromNowhereFailsAndListsWhatIsAvailable() {
        ArchRule check = RuleExclusions.resolvedAgainst(ROOT,
                RuleExclusions.parse("fixture.alfa :: a typo for fixture.alpha", "x"));

        JavaClasses none = nothingToMatch();

        String message = assertThrows(AssertionError.class, () -> check.check(none)).getMessage();

        assertTrue(message.contains("fixture.alfa"), message);
        assertTrue(message.contains("fixture.alpha"),
                () -> "must list the ids it could have meant: " + message);
    }

    /** Every unresolved id at once — fixing the file one round trip per line is how it gets deleted. */
    @Test
    void everyUnresolvedIdIsReportedNotOnlyTheFirst() {
        ArchRule check = RuleExclusions.resolvedAgainst(ROOT, RuleExclusions.parse("""
                fixture.alfa :: one typo
                fixture.bta :: another typo
                """, "x"));

        JavaClasses none = nothingToMatch();

        String message = assertThrows(AssertionError.class, () -> check.check(none)).getMessage();

        assertTrue(message.contains("fixture.alfa"), message);
        assertTrue(message.contains("fixture.bta"), message);
    }

    /**
     * A consumer excluding one of their OWN rules names an id no catalog root publishes. The
     * registry covers them: by the time this check runs their rule class has registered, because
     * their wiring evaluated it.
     */
    /**
     * Registered here rather than relied on from a sibling test: the registry is process-wide and
     * Surefire reuses one JVM, so leaning on another class having run first is exactly the
     * order-dependence {@code PublishedRules} warns about.
     */
    @BeforeAll
    static void registerAConsumerOwnedRuleId() {
        RuleRegistry.register(RuleDoc.builder()
                .id(CONSUMER_OWNED_ID)
                .why("Stands in for a rule the consumer wrote themselves.")
                .howToFix("N/A — this is a test fixture, not a real rule.")
                .build());
    }

    @Test
    void anIdRegisteredByAConsumersOwnRuleResolvesEvenThoughTheRootDoesNotPublishIt() {
        assertFalse(PublishedRules.idsOf(ROOT).contains(CONSUMER_OWNED_ID),
                "the root must NOT publish this id, or the test proves nothing");

        ArchRule check = RuleExclusions.resolvedAgainst(ROOT,
                RuleExclusions.parse(CONSUMER_OWNED_ID + " :: my own rule", "x"));

        assertDoesNotThrow(() -> check.check(nothingToMatch()));
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
                RuleExclusions.parse("fixture.alpha", "x"));

        assertDoesNotThrow(() -> check.check(nothingToMatch()));
    }

    private static JavaClasses nothingToMatch() {
        return new ClassFileImporter().importClasses();
    }
}
