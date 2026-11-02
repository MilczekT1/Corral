package io.github.milczekt1.corral;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.doc.RuleRegistry;
import org.junit.jupiter.api.Test;

class DeprecatedRuleTest {

    private static final JavaClasses ANY = new ClassFileImporter().importClasses(String.class);

    @Test
    void aDeprecatedRuleKeepsItsIdAsTheDescriptionSoExclusionsStillResolve() {
        ArchRule retired = DeprecatedRule.supersededBy(
                "test.class-naming-convention", "corral.test.class-names-must-end-with-test-or-it",
                "renamed to carry a polarity marker");

        assertEquals("test.class-naming-convention", retired.getDescription());
    }

    @Test
    void aDeprecatedRuleAlwaysPasses() {
        ArchRule retired = DeprecatedRule.supersededBy(
                "test.class-naming-convention", "corral.test.class-names-must-end-with-test-or-it",
                "renamed to carry a polarity marker");

        assertDoesNotThrow(() -> retired.check(ANY));
    }

    @Test
    void theReplacementIsNamedInTheDocSoTheBuildLogPointsSomewhere() {
        DeprecatedRule.supersededBy("test.class-naming-convention",
                "corral.test.class-names-must-end-with-test-or-it", "renamed to carry a polarity marker");

        RuleDoc doc = RuleRegistry.find("test.class-naming-convention").orElseThrow();

        assertTrue(doc.howToFix().contains("corral.test.class-names-must-end-with-test-or-it"),
                doc.howToFix());
    }

    @Test
    void aDeprecatedRuleEvaluatesWithoutViolations() {
        ArchRule retired = DeprecatedRule.supersededBy(
                "test.class-naming-convention", "corral.test.class-names-must-end-with-test-or-it",
                "renamed to carry a polarity marker");

        assertFalse(retired.evaluate(ANY).hasViolation());
    }

    @Test
    void becauseAndAllowEmptyShouldKeepTheRetiredIdAsTheDescription() {
        ArchRule retired = DeprecatedRule.supersededBy(
                "test.class-naming-convention", "corral.test.class-names-must-end-with-test-or-it",
                "renamed to carry a polarity marker");

        assertEquals("test.class-naming-convention", retired.because("a reason").getDescription());
        assertEquals("test.class-naming-convention", retired.allowEmptyShould(true).getDescription());
    }

    @Test
    void asRenamesTheDescription() {
        ArchRule retired = DeprecatedRule.supersededBy(
                "test.class-naming-convention", "corral.test.class-names-must-end-with-test-or-it",
                "renamed to carry a polarity marker");

        assertEquals("another-name", retired.as("another-name").getDescription());
    }

    @Test
    void aRetiredIdIsListedForTheGrammarCheckToExempt() {
        DeprecatedRule.supersededBy("test.class-naming-convention",
                "corral.test.class-names-must-end-with-test-or-it", "renamed to carry a polarity marker");

        assertTrue(DeprecatedRule.retiredIds().contains("test.class-naming-convention"),
                DeprecatedRule.retiredIds().toString());
    }
}
