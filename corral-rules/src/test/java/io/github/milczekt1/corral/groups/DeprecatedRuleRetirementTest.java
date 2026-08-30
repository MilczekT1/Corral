package io.github.milczekt1.corral.groups;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.milczekt1.corral.doc.DeprecatedRule;
import io.github.milczekt1.corral.fixtures.RetiredRuleFixtureGroup;
import io.github.milczekt1.corral.reflect.PublishedRules;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * End-to-end coverage for {@link DeprecatedRule#supersededBy} over {@link RetiredRuleFixtureGroup}:
 * a retired id stays published, and passes the catalog's grammar test without being renamed.
 *
 * <p>Walks the fixture directly, so nothing under {@code fixtures/} reaches a real consumer.
 */
class DeprecatedRuleRetirementTest {

    @Test
    void aRetiredIdStaysPublishedAndPassesTheGrammar() {
        Set<String> ids = PublishedRules.idsOf(RetiredRuleFixtureGroup.class);

        assertTrue(ids.contains(RetiredRuleFixtureGroup.RETIRED_ID),
                "supersededBy must keep the retired id evaluated, or an exclusion naming it stops"
                        + " resolving — the whole point of retiring instead of renaming");
        assertTrue(DeprecatedRule.retiredIds().contains(RetiredRuleFixtureGroup.RETIRED_ID),
                "retiredIds() is what lets RuleIdGrammarTest tell \"retired\" from \"wrong\"");

        // The checks RuleIdGrammarTest runs against EveryPublishedGroup, run here against the fixture.
        RuleIdGrammarTest.assertNamespaceGrammar(ids);
        RuleIdGrammarTest.assertPolarityGrammar(ids);
        RuleIdGrammarTest.assertQualifierSegmentGrammar(ids);
    }
}
