package io.github.milczekt1.llamarules.groups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.llamarules.RuleDoc;
import io.github.milczekt1.llamarules.RuleRegistry;
import io.github.milczekt1.llamarules.testsupport.PublishedRules;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
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

    /**
     * One id, one rule. The id is the freeze-store key, so two <em>different</em> rules sharing one
     * would each treat the other's recorded violations as their own.
     *
     * <p>Keyed on rule identity, not on how often an id appears: a rule reachable through two groups
     * is one object read from one {@code static final} field, and enforcing it under both groups is
     * legitimate. {@code RuleRegistry} does not cover this — its guard compares docs, so two rules
     * carrying identical documentation pass it.
     */
    @Test
    void everyRuleIdIsClaimedByExactlyOneRule() {
        Map<String, Set<ArchRule>> rulesById = new LinkedHashMap<>();
        for (ArchRule rule : PublishedRules.all()) {
            rulesById.computeIfAbsent(rule.getDescription(),
                            id -> Collections.newSetFromMap(new IdentityHashMap<>()))
                    .add(rule);
        }

        rulesById.forEach((id, rules) -> assertEquals(1, rules.size(),
                "rule id '" + id + "' is claimed by " + rules.size() + " different rules; ids are"
                        + " freeze-store keys, so those rules would share stored violations"));
    }
}
