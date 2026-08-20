package io.github.milczekt1.llamaguard.fixtures.tree;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.llamaguard.DocumentedRule;
import io.github.milczekt1.llamaguard.doc.RuleDoc;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * A rule that exists only to be found by a reflection walk. Never evaluated, so what it forbids is
 * irrelevant — only its id, which lives in the {@code fixture.} namespace so it cannot collide with
 * a real rule in the process-wide {@code RuleRegistry}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AlphaFixtureRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("fixture.alpha")
            .why("Test fixture. Gives the reflection walk a leaf to find.")
            .howToFix("Nothing to fix — this rule is never evaluated against real code.")
            .build();

    static final ArchRule RULE = noClasses()
            .should().accessField(System.class, "in");

    @ArchTest
    public static final ArchRule rule = new AlphaFixtureRule().guard();

    @Override
    public ArchRule definition() {
        return RULE;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
