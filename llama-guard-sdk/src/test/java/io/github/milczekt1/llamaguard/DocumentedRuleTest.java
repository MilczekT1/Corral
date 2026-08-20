package io.github.milczekt1.llamaguard;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.llamaguard.doc.RuleDoc;
import io.github.milczekt1.llamaguard.doc.RuleRegistry;
import java.util.Optional;
import org.junit.jupiter.api.Test;

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
    void guardDoesNotLeaveTheRawDefinitionDescriptionAsTheDocId() {
        String rawDescription = new FixtureRule().definition().getDescription();

        assertNotEquals(DOC_ID, rawDescription,
                "the raw definition() must not already carry the doc id — guard() is what renames it,"
                        + " and this assertion is what would fail if guard() were simplified into a"
                        + " pass-through");
    }
}
