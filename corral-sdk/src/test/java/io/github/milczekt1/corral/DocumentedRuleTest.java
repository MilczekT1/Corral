package io.github.milczekt1.corral;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import com.tngtech.archunit.library.freeze.ViolationStore;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.doc.RuleRegistry;
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
 * <p>ArchUnit exposes no getter for {@code allowEmptyShould}, so the only way to observe it is to
 * evaluate the rule against classes it matches nothing in. Evaluating a frozen rule needs a store,
 * which {@link FreezingArchRule#persistIn} supplies without touching configuration or the disk —
 * see {@link InMemoryViolationStore}. The assertion is two-sided: dropping the flag must actually
 * break something, or "does not throw" would pass for the wrong reason.
 */
class DocumentedRuleTest {

    private static final String DOC_ID = "fixture.documented-rule-contract";

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

        // Freezing is what the whole design rests on: it is what makes the description the
        // freeze-store key, which is why guard() pins the description to the doc id at all. Without
        // this assertion the freeze(...) wrapper can be deleted and every other test still passes.
        assertInstanceOf(FreezingArchRule.class, guarded,
                "guard() must freeze — an unfrozen rule blocks in-flight work on adoption, and the"
                        + " id would stop being a freeze-store key");
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

    /**
     * The counterpart that gives the assertion above its teeth: the same raw rule, frozen and
     * evaluated the same way but WITHOUT {@code allowEmptyShould(true)}, must fail. If ArchUnit ever
     * stops failing on an empty should by default, this test goes red rather than its sibling
     * quietly passing for the wrong reason.
     */
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
     * {@link FreezingArchRule#persistIn}.
     *
     * <p>Keeps these tests off both ArchUnit's global configuration and the filesystem: a store
     * registered through {@code freeze.store} is process-wide, and Surefire reuses one JVM, so
     * setting it here would leak into every sibling test.
     *
     * <p>An empty store means the rule is unknown, which is the case under test — a rule being
     * adopted for the first time seeds whatever it finds and passes.
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
