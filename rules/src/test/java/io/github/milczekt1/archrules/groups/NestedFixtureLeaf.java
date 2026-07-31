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
 * <p>Never referenced by {@link AllCentralRules}, never wired into any {@code @AnalyzeClasses}
 * test class, so its {@code @ArchTest} field never actually runs — it exists solely so a test can
 * prove {@code AllCentralRules.loadAll()} and {@code PublishedRules.rulesReachableFrom} both
 * descend into a genuinely nested arrangement, without moving or touching a real rule.
 *
 * <p>A top-level public class (not nested in the test class) because reflective {@code Field.get}
 * requires the declaring class itself to be accessible from {@code PublishedRules}' package.
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
