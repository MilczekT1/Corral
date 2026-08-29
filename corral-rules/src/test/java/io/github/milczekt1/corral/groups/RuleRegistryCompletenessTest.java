package io.github.milczekt1.corral.groups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.doc.RuleRegistry;
import io.github.milczekt1.corral.reflect.PublishedRules;
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
 * <p>Everything here is keyed on {@link PublishedRules} rather than {@code RuleRegistry.all()},
 * which is process-wide static state that sibling tests pollute in a reused JVM.
 */
class RuleRegistryCompletenessTest {

    @Test
    void everyPublishedRuleHasARegisteredDoc() {
        for (ArchRule rule : PublishedRules.rulesReachableFrom(EveryPublishedGroup.class)) {
            String description = rule.getDescription();
            assertTrue(RuleRegistry.find(description).isPresent(),
                    "rule description '" + description + "' is not a registered RuleDoc id — the failure"
                            + " formatter would fall back to plain ArchUnit output for it");
        }
    }

    @Test
    void everyPublishedRuleHasUsableGuidance() {
        for (ArchRule rule : PublishedRules.rulesReachableFrom(EveryPublishedGroup.class)) {
            String description = rule.getDescription();
            Optional<RuleDoc> found = RuleRegistry.find(description);

            assertTrue(found.isPresent(), "no doc registered for published rule '" + description + "'");
            RuleDoc doc = found.get();
            assertFalse(doc.why().isBlank(), doc.id() + " has a blank why");
            assertFalse(doc.howToFix().isBlank(), doc.id() + " has a blank howToFix");
        }
    }

    /**
     * One id, one rule: the id is the freeze-store key.
     *
     * <p>Keyed on rule identity, not on how often an id appears — a rule reachable through two
     * groups is one object read from one {@code static final} field. {@code RuleRegistry} does not
     * cover this: its guard compares docs, not rules.
     */
    @Test
    void everyRuleIdIsClaimedByExactlyOneRule() {
        Map<String, Set<ArchRule>> rulesById = new LinkedHashMap<>();
        for (ArchRule rule : PublishedRules.rulesReachableFrom(EveryPublishedGroup.class)) {
            rulesById.computeIfAbsent(rule.getDescription(),
                            id -> Collections.newSetFromMap(new IdentityHashMap<>()))
                    .add(rule);
        }

        rulesById.forEach((id, rules) -> assertEquals(1, rules.size(),
                "rule id '" + id + "' is claimed by " + rules.size() + " different rules; ids are"
                        + " freeze-store keys, so those rules would share stored violations"));
    }
}
