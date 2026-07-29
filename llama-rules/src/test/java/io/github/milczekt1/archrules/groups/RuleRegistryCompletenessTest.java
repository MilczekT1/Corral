package io.github.milczekt1.archrules.groups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.archrules.RuleDoc;
import io.github.milczekt1.archrules.RuleRegistry;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RuleRegistryCompletenessTest {

    @BeforeAll
    static void loadEveryGroup() {
        AllCentralRules.loadAll();
    }

    /** Every {@code @ArchTest ArchRule} field across every group class. */
    private static List<ArchRule> publishedRules() {
        List<ArchRule> rules = new ArrayList<>();
        for (Class<?> group : AllCentralRules.groups()) {
            for (Field field : group.getDeclaredFields()) {
                if (field.isAnnotationPresent(ArchTest.class)
                        && ArchRule.class.isAssignableFrom(field.getType())
                        && Modifier.isStatic(field.getModifiers())) {
                    try {
                        rules.add((ArchRule) field.get(null));
                    } catch (IllegalAccessException e) {
                        throw new AssertionError("rule field must be public: " + field, e);
                    }
                }
            }
        }
        return rules;
    }

    @Test
    void everyPublishedRuleHasARegisteredDoc() {
        for (ArchRule rule : publishedRules()) {
            String description = rule.getDescription();
            assertTrue(RuleRegistry.find(description).isPresent(),
                    "rule description '" + description + "' is not a registered RuleDoc id — the failure"
                            + " formatter would fall back to plain ArchUnit output for it");
        }
    }

    @Test
    void everyRegisteredDocHasUsableGuidance() {
        for (RuleDoc doc : RuleRegistry.all()) {
            assertFalse(doc.why().isBlank(), doc.id() + " has a blank why");
            assertFalse(doc.howToFix().isBlank(), doc.id() + " has a blank howToFix");
        }
    }

    @Test
    void ruleIdsAreUnique() {
        List<String> ids = publishedRules().stream().map(ArchRule::getDescription).toList();
        Set<String> unique = new LinkedHashSet<>(ids);

        assertEquals(ids.size(), unique.size(), "duplicate rule ids among published rules: " + ids);
    }

    @Test
    void publishesExactlyTheSeededFirstCutRules() {
        // Locks the first-cut scope. Adding a rule is a deliberate edit here AND in the README.
        Set<String> ids = new LinkedHashSet<>(publishedRules().stream().map(ArchRule::getDescription).toList());

        assertEquals(Set.of(
                "db.no-spring-transactional-on-classes",
                "db.no-spring-transactional-on-methods",
                "db.no-raw-jdbc-outside-repositories",
                "test.no-mocked-repository-in-integration-test",
                "test.class-naming-convention"), ids);
    }

    @Test
    void aggregatorExposesEveryGroup() {
        assertEquals(List.of(DatabaseRules.class, TestingRules.class), AllCentralRules.groups());
    }
}
