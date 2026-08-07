package io.github.milczekt1.archrules.groups;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.archrules.RuleDoc;
import io.github.milczekt1.archrules.RuleRegistry;

/**
 * Fixture-only leaf for {@link RuleRegistryCompletenessTest}: a rule class reached only through
 * {@link NestedFixtureGroup}'s {@code @ArchTest ArchTests} field, the shape real rules have after
 * the rule-per-class move.
 *
 * <p>Unreachable from {@link AllCentralRules} and wired into no {@code @AnalyzeClasses} class, so it
 * never runs. It exists so tests can prove {@code loadAll()} and {@code rulesReachableFrom} descend
 * into real nesting without touching a production rule.
 *
 * <p>Top-level and public because reflective {@code Field.get} needs the declaring class accessible
 * from {@code PublishedRules}' package.
 */
public final class NestedFixtureLeaf {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("test.nested-fixture-registry-propagation-check")
            .why("Fixture-only: proves RuleRegistry is populated, and the rule is discoverable, for "
                    + "a rule reached solely through a nested @ArchTest ArchTests field.")
            .howToFix("N/A — never wired into any @AnalyzeClasses test class, so it never runs.")
            .build();

    static {
        RuleRegistry.register(DOC);
    }

    @ArchTest
    public static final ArchRule rule = classes().should().bePublic().as(DOC.id()).allowEmptyShould(true);

    private NestedFixtureLeaf() {
    }
}
