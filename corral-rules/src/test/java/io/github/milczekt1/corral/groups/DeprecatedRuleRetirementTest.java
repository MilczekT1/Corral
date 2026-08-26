package io.github.milczekt1.corral.groups;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.milczekt1.corral.doc.DeprecatedRule;
import io.github.milczekt1.corral.fixtures.RetiredRuleFixtureGroup;
import io.github.milczekt1.corral.reflect.PublishedRules;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * End-to-end coverage for {@link DeprecatedRule#supersededBy}, wired exactly as its Javadoc
 * instructs: {@link RetiredRuleFixtureGroup} holds one {@code @ArchTest ArchRule} field built from a
 * retired id with no polarity marker — the shape a rule id had before the grammar existed.
 *
 * <p>Two things must both hold, or the mechanism does not do what it exists for: the retired id
 * keeps being published (so an exclusion naming it keeps resolving), and it does not fail the
 * catalog's own grammar test (so the maintainer is never told to "fix" it by renaming it — the one
 * action retiring instead of renaming exists to forbid).
 *
 * <p>Never wires {@link RetiredRuleFixtureGroup} into {@code AllCentralRules}: this test walks the
 * fixture directly, so nothing under {@code fixtures/} ever reaches a real consumer.
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

        // The very checks RuleIdGrammarTest runs against AllCentralRules, run here against the
        // fixture: proof that a retired id — which does not conform to the grammar — passes once
        // DeprecatedRule.retiredIds() exempts it, rather than trusting a re-implementation of the
        // same rule to agree with the original.
        RuleIdGrammarTest.assertNamespaceGrammar(ids);
        RuleIdGrammarTest.assertPolarityGrammar(ids);
        RuleIdGrammarTest.assertQualifierSegmentGrammar(ids);
    }
}
