package io.github.milczekt1.llamaguard;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.llamaguard.doc.RuleDoc;
import io.github.milczekt1.llamaguard.doc.RuleRegistry;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Pins the four things {@link DocumentedRule#guard()} promises: the doc is registered, the
 * description is the doc id, the rule is frozen, and empty {@code should}s are allowed.
 *
 * <p><strong>Known gap: {@code allowEmptyShould(true)} is not asserted here.</strong> ArchUnit
 * exposes no getter for it — the only way to observe it is to evaluate the rule against classes it
 * matches nothing in and see whether it passes or raises, and because {@code guard()} also freezes,
 * evaluating means standing up a throwaway violation store and pointing ArchUnit's configuration at
 * it. That machinery would cost more than the assertion is worth and would couple this test to the
 * freeze store's on-disk format. Recorded here rather than left silent: if that flag is dropped, no
 * test in this repo goes red, and a consumer module with no matching classes starts failing.
 */
class DocumentedRuleTest {

    private static final String DOC_ID = "fixture.documented-rule-contract";

    private static final class FixtureRule implements DocumentedRule {

        private static final ArchRule RULE = noClasses().should().accessField(System.class, "in");

        private static final RuleDoc DOC = RuleDoc.builder()
                .id(DOC_ID)
                .why("Verifies the DocumentedRule#guard() contract directly.")
                .howToFix("N/A — this is a test fixture, not a real rule.")
                .build();

        @Override
        public ArchRule definition() {
            return RULE;
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
}
