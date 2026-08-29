package io.github.milczekt1.corral;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import com.tngtech.archunit.library.freeze.ViolationStore;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.doc.RuleRegistry;
import io.github.milczekt1.corral.exclude.RuleExclusions;
import io.github.milczekt1.corral.guard.IgnorePatternsGuard;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/**
 * Pins the four things {@link DocumentedRule#guard()} promises: the doc is registered, the
 * description is the doc id, the rule is frozen, and empty {@code should}s are allowed.
 *
 * <p>{@code allowEmptyShould} has no getter, so it is observed by evaluating a rule that matches
 * nothing, against an {@link InMemoryViolationStore} handed over by
 * {@link FreezingArchRule#persistIn}.
 */
class DocumentedRuleTest {

    private static final String DOC_ID = "test.no-documented-rule-contract-fixture";

    private static final class FixtureRule implements DocumentedRule {

        private static final ArchRule DEFINITION = noClasses().should().accessField(System.class, "in");

        private static final RuleDoc DOC = RuleDoc.builder()
                .id(DOC_ID)
                .why("Verifies the DocumentedRule#guard() contract directly.")
                .howToFix("N/A — this is a test fixture, not a real rule.")
                .build();

        @Override
        public ArchRule definition() {
            return DEFINITION;
        }

        @Override
        public RuleDoc doc() {
            return DOC;
        }
    }

    @Test
    void guardPinsTheRuleDescriptionToTheDocId() {
        ArchRule guarded = new FixtureRule().guard();

        assertEquals(DOC_ID, guarded.getDescription());
    }

    @Test
    void guardRegistersTheDocSoItCanBeFoundById() {
        new FixtureRule().guard();

        assertEquals(Optional.of(FixtureRule.DOC), RuleRegistry.find(DOC_ID));
    }

    @Test
    void guardFreezesTheRuleSoAdoptingItRecordsExistingDebt() {
        ArchRule guarded = new FixtureRule().guard();

        assertInstanceOf(FreezingArchRule.class, guarded,
                "guard() must freeze — an unfrozen rule blocks in-flight work on adoption, and the"
                        + " id would stop being a freeze-store key");
    }

    /**
     * The wiring, not the detection — {@code IgnorePatternsGuardTest} owns that. Asserted against
     * the bytecode, since on a clean classpath the call is a no-op.
     */
    @Test
    void guardInterposesTheIgnorePatternsCheckOnEveryRule() {
        JavaMethod guard = new ClassFileImporter()
                .importClasses(DocumentedRule.class)
                .get(DocumentedRule.class)
                .getMethod("guard");

        assertTrue(guard.getMethodCallsFromSelf().stream()
                        .anyMatch(call -> call.getTargetOwner().isEquivalentTo(IgnorePatternsGuard.class)
                                && call.getName().equals("interposeOn")),
                "guard() must run the ignore-patterns check — it is the only place a whole-catalog"
                        + " kill switch cannot hide from");
    }

    /** The wiring, not the mechanism — {@code RuleExclusionsGuardTest} owns that. Bytecode, as above. */
    @Test
    void guardAppliesExclusionsToEveryRule() {
        JavaMethod guard = new ClassFileImporter()
                .importClasses(DocumentedRule.class)
                .get(DocumentedRule.class)
                .getMethod("guard");

        assertTrue(guard.getMethodCallsFromSelf().stream()
                        .anyMatch(call -> call.getTargetOwner().isEquivalentTo(RuleExclusions.class)
                                && call.getName().equals("applyTo")),
                "guard() must apply exclusions — it is the one place every rule passes through");
    }

    /** The wiring, not the mechanism — {@code RuleExclusionsWarningTest} owns that. Bytecode, as above. */
    @Test
    void guardWarnsOnUnmatchedExclusionsOnEveryRule() {
        JavaMethod guard = new ClassFileImporter()
                .importClasses(DocumentedRule.class)
                .get(DocumentedRule.class)
                .getMethod("guard");

        assertTrue(guard.getMethodCallsFromSelf().stream()
                        .anyMatch(call -> call.getTargetOwner().isEquivalentTo(RuleExclusions.class)
                                && call.getName().equals("warnUnmatchedExclusionsOnFirstEvaluation")),
                "guard() must warn on an unmatched exclusion — it is the one place every rule passes"
                        + " through, so it is the one place that needs no wired root");
    }

    /** The exclusion wrapper must sit outside the freeze, or an excluded rule reaches the store. */
    @Test
    void exclusionsAreAppliedOutsideTheFreezeSoAnExcludedRuleNeverTouchesTheStore() {
        JavaMethod guard = new ClassFileImporter()
                .importClasses(DocumentedRule.class)
                .get(DocumentedRule.class)
                .getMethod("guard");
        List<String> callOrder = guard.getMethodCallsFromSelf().stream()
                .sorted(Comparator.comparingInt(JavaAccess::getLineNumber))
                .map(call -> call.getTargetOwner().getSimpleName() + "." + call.getName())
                .toList();

        int freeze = callOrder.indexOf("FreezingArchRule.freeze");
        int apply = callOrder.indexOf("RuleExclusions.applyTo");

        assertTrue(freeze >= 0 && apply >= 0, () -> "expected both calls, got " + callOrder);
        assertTrue(freeze < apply,
                () -> "freeze(...) must be evaluated before applyTo wraps it, so the exclusion sits"
                        + " outside the freeze: " + callOrder);
    }

    @Test
    void guardDoesNotLeaveTheRawDefinitionDescriptionAsTheDocId() {
        String rawDescription = new FixtureRule().definition().getDescription();

        assertNotEquals(DOC_ID, rawDescription,
                "the raw definition() must not already carry the doc id — guard() is what renames it,"
                        + " and this assertion is what would fail if guard() were simplified into a"
                        + " pass-through");
    }

    @Test
    void guardAllowsAnEmptyShouldSoAModuleWithNoMatchingClassesStaysGreen() {
        FreezingArchRule guarded = assertInstanceOf(FreezingArchRule.class, new FixtureRule().guard());
        ArchRule guardedWithStore = guarded.persistIn(new InMemoryViolationStore());
        JavaClasses noClassesAtAll = nothingToMatch();

        assertDoesNotThrow(() -> guardedWithStore.check(noClassesAtAll),
                "guard() must allow an empty should — without it, adopting a rule turns every module"
                        + " with no matching classes red for a reason the consumer cannot act on");
    }

    /** The counterpart: the same rule without {@code allowEmptyShould(true)} must fail. */
    @Test
    void withoutAllowEmptyShouldTheSameRuleFailsOnAModuleWithNoMatchingClasses() {
        ArchRule withoutTheFlag = FreezingArchRule.freeze(
                        FixtureRule.DEFINITION.as("fixture.documented-rule-contract-without-allow-empty-should"))
                .persistIn(new InMemoryViolationStore());
        JavaClasses noClassesAtAll = nothingToMatch();

        assertThrows(AssertionError.class, () -> withoutTheFlag.check(noClassesAtAll));
    }

    /** No classes at all — the shape of a consumer module the rule's {@code should} cannot match. */
    private static JavaClasses nothingToMatch() {
        return new ClassFileImporter().importClasses();
    }

    /**
     * A {@link ViolationStore} held in a map, handed to one rule via
     * {@link FreezingArchRule#persistIn} — a {@code freeze.store} would be process-wide and leak
     * across the Surefire JVM.
     *
     * <p>Empty, so the rule reads as unknown: it seeds whatever it finds and passes.
     */
    private static final class InMemoryViolationStore implements ViolationStore {

        private final Map<String, List<String>> violationsByRuleDescription = new HashMap<>();

        @Override
        public void initialize(Properties properties) {
            // Nothing to configure: the map is the store.
        }

        @Override
        public boolean contains(ArchRule rule) {
            return violationsByRuleDescription.containsKey(rule.getDescription());
        }

        @Override
        public void save(ArchRule rule, List<String> violations) {
            violationsByRuleDescription.put(rule.getDescription(), List.copyOf(violations));
        }

        @Override
        public List<String> getViolations(ArchRule rule) {
            return violationsByRuleDescription.getOrDefault(rule.getDescription(), List.of());
        }
    }
}
