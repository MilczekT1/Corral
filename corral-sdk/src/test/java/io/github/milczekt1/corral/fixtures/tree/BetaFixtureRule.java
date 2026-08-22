package io.github.milczekt1.corral.fixtures.tree;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Second fixture rule. See {@link AlphaFixtureRule}. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BetaFixtureRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("fixture.beta")
            .why("Test fixture. Gives the reflection walk a second, distinct leaf.")
            .howToFix("Nothing to fix — this rule is never evaluated against real code.")
            .build();

    static final ArchRule DEFINITION = noClasses()
            .should().accessField(System.class, "in");

    @ArchTest
    public static final ArchRule rule = new BetaFixtureRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
