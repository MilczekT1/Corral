package io.github.milczekt1.corral.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.corral.rules.testing.nomutablestaticstate.NoMutableStaticStateRule;
import lombok.experimental.UtilityClass;

/**
 * Every rule the catalog ships outside any group, as one root the invariant tests here can walk.
 *
 * <p>A rule stands alone rather than joining a group when it is sound but not right for every
 * codebase: a group is adopted whole, so a rule inside one runs for everybody who wires that group.
 * Consumers reach a standalone rule by naming its class — {@code ArchTests.in(TheRule.class)} —
 * which is exactly what a group's field does, so nothing about the rule itself differs.
 *
 * <p>Test-scoped, like {@link EveryPublishedGroup}, and listed for the same reason: without it the
 * grammar, doc and id-uniqueness checks would never see these rules. Nothing fails when a rule is
 * added to the catalog and not listed below — {@code RulesCatalogDocTest} only catches it once the
 * rule has a row in {@code docs/rules.md}.
 */
@UtilityClass
public class EveryStandaloneRule {

    @ArchTest
    public static final ArchTests noMutableStaticState = ArchTests.in(NoMutableStaticStateRule.class);
}
