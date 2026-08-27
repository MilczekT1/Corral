package io.github.milczekt1.corral.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.exclude.RuleExclusions;
import lombok.experimental.UtilityClass;

/**
 * Framework plumbing, not architecture rules: checks that validate how Corral itself is configured
 * rather than anything about the consumer's code.
 *
 * <p>{@link #everyExclusionNamesARealRule} is the check that every line of {@code corral-exclusions.txt}
 * names a rule that exists. It is wired here — a member of {@link AllCentralRules} — rather than in
 * {@code guard()} because only a walk from that root sees the whole catalog — mid-run, a rule being
 * evaluated can only see the rules loaded before it.
 *
 * <p><b>Mutual reference, deliberately.</b> {@link AllCentralRules} points at this class, and this
 * class points back at {@code AllCentralRules.class}. That is safe: both sides only ever need the
 * {@code Class} object, never a field value, so neither class's static initialiser has to wait on the
 * other. {@link io.github.milczekt1.corral.reflect.PublishedRules#idsOf} walks {@code @ArchTest}
 * fields at <em>evaluation</em> time — long after both classes have finished initialising — so there
 * is no ordering hazard here despite how this codebase has been bitten by class-init ordering before.
 * Do not "fix" this by trying to break the cycle.
 */
@UtilityClass
public class ConfigurationChecksGroup {

    /**
     * Fails when {@code corral-exclusions.txt} names an id nothing here publishes and nothing has
     * registered — a typo, or a rule renamed upstream. Either way the line removes nothing while
     * reading in the diff as though it did.
     *
     * <p>The argument is {@code AllCentralRules.class}, not this class: the wired root is what
     * defines the complete set of known ids, and this class alone publishes only this one guard.
     */
    @ArchTest
    public static final ArchRule everyExclusionNamesARealRule =
            RuleExclusions.resolvedAgainst(AllCentralRules.class);
}
