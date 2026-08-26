package io.github.milczekt1.corral.fixtures;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.doc.DeprecatedRule;
import lombok.experimental.UtilityClass;

/**
 * Exercises {@link DeprecatedRule#supersededBy} exactly as its Javadoc instructs a maintainer to:
 * an {@code @ArchTest ArchRule} field built from a deliberately non-conforming retired id — no
 * polarity marker, the shape a rule id had before {@code RuleIdGrammarTest}'s grammar existed.
 *
 * <p>Never wired into {@code AllCentralRules} — {@code DeprecatedRuleRetirementTest} in
 * {@code groups} walks this class directly. Under {@code fixtures/}, so Surefire's
 * {@code **&#47;fixtures/**} exclusion keeps this class itself from ever running as a test.
 */
@UtilityClass
public class RetiredRuleFixtureGroup {

    /** Predates the grammar on purpose: no {@code no-}/{@code -must-} marker on the slug. */
    public static final String RETIRED_ID = "test.class-naming-convention";

    @ArchTest
    public static final ArchRule retired = DeprecatedRule.supersededBy(
            RETIRED_ID, "test.class-names-must-end-with-test-or-it",
            "fixture: proves the retirement path stays usable end to end");
}
