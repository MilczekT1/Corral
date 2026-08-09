package io.github.milczekt1.llamarules.groups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.llamarules.RuleDoc;
import io.github.milczekt1.llamarules.RuleRegistry;
import io.github.milczekt1.llamarules.testsupport.PublishedRules;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Guards the contract between the rules consumers evaluate and their documentation.
 *
 * <p>Everything here is keyed on {@link PublishedRules} rather than {@code RuleRegistry.all()}.
 * The registry is process-wide static state and Surefire reuses one JVM, so sibling tests that
 * register throwaway docs pollute it; asserting over its total contents would make these tests
 * depend on run order.
 */
class RuleRegistryCompletenessTest {

    @Test
    void everyPublishedRuleHasARegisteredDoc() {
        for (ArchRule rule : PublishedRules.all()) {
            String description = rule.getDescription();
            assertTrue(RuleRegistry.find(description).isPresent(),
                    "rule description '" + description + "' is not a registered RuleDoc id — the failure"
                            + " formatter would fall back to plain ArchUnit output for it");
        }
    }

    @Test
    void everyPublishedRuleHasUsableGuidance() {
        for (ArchRule rule : PublishedRules.all()) {
            String description = rule.getDescription();
            Optional<RuleDoc> found = RuleRegistry.find(description);

            assertTrue(found.isPresent(), "no doc registered for published rule '" + description + "'");
            RuleDoc doc = found.get();
            assertFalse(doc.why().isBlank(), doc.id() + " has a blank why");
            assertFalse(doc.howToFix().isBlank(), doc.id() + " has a blank howToFix");
        }
    }

    @Test
    void ruleIdsAreUnique() {
        List<String> ids = PublishedRules.ids();
        Set<String> unique = new LinkedHashSet<>(ids);

        assertEquals(ids.size(), unique.size(), "duplicate rule ids among published rules: " + ids);
    }
}
