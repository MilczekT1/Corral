package io.github.milczekt1.corral.doc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class DeprecatedRuleTest {

    private static final JavaClasses ANY = new ClassFileImporter().importClasses(String.class);

    @Test
    void aDeprecatedRuleKeepsItsIdAsTheDescriptionSoExclusionsStillResolve() {
        ArchRule retired = DeprecatedRule.supersededBy(
                "test.class-naming-convention", "test.class-names-must-end-with-test-or-it",
                "renamed to carry a polarity marker");

        assertEquals("test.class-naming-convention", retired.getDescription());
    }

    @Test
    void aDeprecatedRuleAlwaysPasses() {
        ArchRule retired = DeprecatedRule.supersededBy(
                "test.class-naming-convention", "test.class-names-must-end-with-test-or-it",
                "renamed to carry a polarity marker");

        assertDoesNotThrow(() -> retired.check(ANY));
    }

    @Test
    void theReplacementIsNamedInTheDocSoTheBuildLogPointsSomewhere() {
        DeprecatedRule.supersededBy("test.class-naming-convention",
                "test.class-names-must-end-with-test-or-it", "renamed to carry a polarity marker");

        RuleDoc doc = RuleRegistry.find("test.class-naming-convention").orElseThrow();

        assertTrue(doc.howToFix().contains("test.class-names-must-end-with-test-or-it"),
                doc.howToFix());
    }
}
