package io.github.milczekt1.corral.exclude;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import com.tngtech.archunit.library.freeze.ViolationStore;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.doc.RuleRegistry;
import io.github.milczekt1.corral.exclude.RuleExclusions.Loaded;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * What an exclusion does to a rule. {@code RuleExclusionsFileTest} owns the file; this owns the
 * effect — the two directions that matter are that a named rule stops evaluating and that every
 * rule it does not name is handed back untouched, identical object included.
 */
class RuleExclusionsGuardTest {

    private static final String EXCLUDED_ID = "test.no-guard-excluded-fixture";
    private static final String OTHER_ID = "test.no-guard-other-fixture";

    /** Violated by every class there is, so "evaluated nothing" is distinguishable from "found nothing". */
    private static final ArchRule ALWAYS_VIOLATED =
            noClasses().should().haveNameMatching(".*").as(EXCLUDED_ID);

    /**
     * Excluding an id no rule registers is itself a failure, so the fixture ids must be real
     * registered docs — otherwise every test here would fail on the unknown-id check instead of on
     * what it means to assert.
     */
    @BeforeAll
    static void registerTheFixtureIds() {
        RuleRegistry.register(docFor(EXCLUDED_ID));
        RuleRegistry.register(docFor(OTHER_ID));
    }

    private static RuleDoc docFor(String id) {
        return RuleDoc.builder()
                .id(id)
                .why("A fixture for RuleExclusionsGuardTest.")
                .howToFix("N/A — this is a test fixture, not a real rule.")
                .build();
    }

    private static final Loaded EXCLUDING = RuleExclusions.parse(
            EXCLUDED_ID + " :: this rule contradicts how this codebase is built", "test-source");

    @Test
    void anExcludedRuleEvaluatesNothingAndPasses() {
        JavaClasses everything = someClasses();
        assertTrue(ALWAYS_VIOLATED.evaluate(everything).hasViolation(),
                "the fixture rule must fail unexcluded, or the assertion below proves nothing");

        ArchRule excluded = RuleExclusions.applyTo(ALWAYS_VIOLATED, EXCLUDED_ID, EXCLUDING);

        assertDoesNotThrow(() -> excluded.check(everything));
        assertFalse(excluded.evaluate(everything).hasViolation());
    }

    @Test
    void withNoFileAtAllEveryRuleIsHandedBackUntouched() {
        assertSame(ALWAYS_VIOLATED,
                RuleExclusions.applyTo(ALWAYS_VIOLATED, EXCLUDED_ID, Loaded.none()));
    }

    /**
     * The description is the freeze-store key, so an excluded rule must keep it — a consumer who
     * re-enables the rule finds their store entry where they left it.
     */
    @Test
    void anExcludedRuleKeepsItsDescription() {
        ArchRule excluded = RuleExclusions.applyTo(ALWAYS_VIOLATED, EXCLUDED_ID, EXCLUDING);

        assertEquals(EXCLUDED_ID, excluded.getDescription());
        assertEquals("renamed", excluded.as("renamed").getDescription());
    }

    /** An excluded rule that quietly wrote to the freeze store would corrupt the debt record. */
    @Test
    void anExcludedRuleReportsNoViolationsToWriteToTheStore() {
        var result = RuleExclusions.applyTo(ALWAYS_VIOLATED, EXCLUDED_ID, EXCLUDING)
                .evaluate(someClasses());

        assertTrue(result.getFailureReport().getDetails().isEmpty(),
                () -> "an excluded rule must produce no violation lines: " + result.getFailureReport());
    }

    /**
     * A file that cannot be understood must not be trusted to remove a rule, so it removes none and
     * fails all of them — including rules no line ever named.
     */
    @Test
    void abrokenFileFailsEveryRuleNamingTheProblem() {
        Loaded broken = RuleExclusions.parse("logging.no-system-out", "file:/somewhere/x.txt");
        JavaClasses none = nothingToMatch();

        ArchRule named = RuleExclusions.applyTo(ALWAYS_VIOLATED, EXCLUDED_ID, broken);
        ArchRule unnamed = RuleExclusions.applyTo(ALWAYS_VIOLATED, OTHER_ID, broken);

        String message = assertThrows(AssertionError.class, () -> named.check(none)).getMessage();
        assertTrue(message.contains(broken.problem()), message);
        assertThrows(AssertionError.class, () -> unnamed.check(none));
    }

    /** Nothing a caller can chain may soften a broken file into something that passes. */
    @Test
    void abrokenFileCannotBeChainedIntoPassing() {
        Loaded broken = RuleExclusions.parse("logging.no-system-out", "file:/somewhere/x.txt");
        ArchRule failing = RuleExclusions.applyTo(ALWAYS_VIOLATED, EXCLUDED_ID, broken);
        JavaClasses none = nothingToMatch();

        ArchRule because = failing.because("we need it");
        ArchRule allowingEmpty = failing.allowEmptyShould(true);

        assertNotSame(ALWAYS_VIOLATED, failing);
        assertThrows(AssertionError.class, () -> because.check(none));
        assertThrows(AssertionError.class, () -> allowingEmpty.check(none));
        assertThrows(AssertionError.class, () -> failing.evaluate(none));
    }

    /**
     * A run that does not load every rule-declaring class sees a partial {@link RuleRegistry} — one
     * test class from the IDE gutter, a single {@code -Dtest=...}, a forked-per-class Surefire. A
     * valid exclusion naming a rule that run never loads must NOT be treated as a typo, and must not
     * fail the rules that did run. Corral advertises "runnable at any granularity"; this is that.
     */
    @Test
    void aRunThatNeverLoadsTheExcludedRuleStillPasses() {
        Loaded namesARuleNotInThisRun = RuleExclusions.parse(
                "fixture.absent-from-this-run :: a real rule, in a group this run does not wire",
                "file:/somewhere/x.txt");
        ArchRule other = noClasses().should().haveNameMatching(".*").as(OTHER_ID);
        JavaClasses everything = someClasses();

        ArchRule applied = RuleExclusions.applyTo(other, OTHER_ID, namesARuleNotInThisRun);

        assertEquals(other.evaluate(everything).getFailureReport().getDetails(),
                applied.evaluate(everything).getFailureReport().getDetails(),
                "a rule that ran must report exactly what it would have, not fail on another"
                        + " rule's exclusion being unresolvable in this run");
    }

    /** With the unknown-id check gone from evaluation, an unnamed rule is untouched again. */
    @Test
    void aRuleNoLineNamesIsHandedBackUntouched() {
        ArchRule other = noClasses().should().haveNameMatching(".*").as(OTHER_ID);

        assertSame(other, RuleExclusions.applyTo(other, OTHER_ID, EXCLUDING));
    }

    /**
     * Not a pause button. Excluding a rule must leave the freeze store alone rather than re-record
     * it as clean — a rewrite would delete the debt entries and resurface them all on re-enabling.
     */
    @Test
    void anExcludedRuleNeverWritesToTheFreezeStore() {
        RecordingViolationStore store = new RecordingViolationStore();
        ArchRule frozen = FreezingArchRule.freeze(ALWAYS_VIOLATED).persistIn(store);

        RuleExclusions.applyTo(frozen, EXCLUDED_ID, EXCLUDING).check(someClasses());

        assertTrue(store.saved.isEmpty(), () -> "an excluded rule wrote to the store: " + store.saved);
    }

    /** Counterpart: unexcluded, the same rule does write — or the assertion above proves nothing. */
    @Test
    void theSameRuleUnexcludedDoesWriteToTheFreezeStore() {
        RecordingViolationStore store = new RecordingViolationStore();
        ArchRule frozen = FreezingArchRule.freeze(ALWAYS_VIOLATED).persistIn(store);

        RuleExclusions.applyTo(frozen, EXCLUDED_ID, Loaded.none()).check(someClasses());

        assertFalse(store.saved.isEmpty());
    }

    /** An empty store means the rule is unknown: freezing seeds whatever it finds and passes. */
    private static final class RecordingViolationStore implements ViolationStore {

        private final Map<String, List<String>> saved = new HashMap<>();

        @Override
        public void initialize(Properties properties) {
            // Nothing to configure: the map is the store.
        }

        @Override
        public boolean contains(ArchRule rule) {
            return saved.containsKey(rule.getDescription());
        }

        @Override
        public void save(ArchRule rule, List<String> violations) {
            saved.put(rule.getDescription(), List.copyOf(violations));
        }

        @Override
        public List<String> getViolations(ArchRule rule) {
            return saved.getOrDefault(rule.getDescription(), List.of());
        }
    }

    private static JavaClasses someClasses() {
        return new ClassFileImporter().importClasses(RuleExclusions.class, Exclusion.class);
    }

    private static JavaClasses nothingToMatch() {
        return new ClassFileImporter().importClasses();
    }
}
