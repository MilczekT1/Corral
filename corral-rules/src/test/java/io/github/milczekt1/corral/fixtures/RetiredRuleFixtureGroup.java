package io.github.milczekt1.corral.fixtures;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.doc.DeprecatedRule;
import lombok.experimental.UtilityClass;

/**
 * Wires {@link DeprecatedRule#supersededBy} as its Javadoc instructs, from a retired id that predates
 * {@code RuleIdGrammarTest}'s grammar.
 *
 * <p>Never wired into {@code AllCentralRules}; {@code DeprecatedRuleRetirementTest} walks it directly.
 * Under {@code fixtures/}, which Surefire excludes from running as tests.
 */
@UtilityClass
public class RetiredRuleFixtureGroup {

    /** Predates the grammar: no {@code no-}/{@code -must-} marker on the slug. */
    public static final String RETIRED_ID = "test.class-naming-convention";

    @ArchTest
    public static final ArchRule retired = DeprecatedRule.supersededBy(
            RETIRED_ID, "corral.test.class-names-must-end-with-test-or-it",
            "fixture: proves the retirement path stays usable end to end");
}
