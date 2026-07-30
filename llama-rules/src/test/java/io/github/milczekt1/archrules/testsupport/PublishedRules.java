package io.github.milczekt1.archrules.testsupport;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.archrules.groups.AllCentralRules;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Reflection over what consumers actually evaluate: the {@code @ArchTest} fields of the group
 * classes listed in {@link AllCentralRules#groups()}.
 *
 * <p>Exists so tests can assert against the <em>published</em> rule set rather than
 * {@code RuleRegistry.all()}. The registry is deliberately process-wide static state and Surefire
 * reuses one JVM, so any sibling test that registers a throwaway {@code RuleDoc} leaks into it —
 * a test keyed on the registry's total contents is therefore order-dependent. Keying on the
 * published fields is order-independent <em>and</em> a stronger assertion, because it pins exactly
 * what a consumer runs.
 *
 * <p>Test-only helper: nothing in production is made public for its sake. Not named {@code *Test},
 * so Surefire does not try to execute it.
 */
public final class PublishedRules {

    private PublishedRules() {
    }

    /** Every {@code @ArchTest ArchRule} field across every group class, in group order. */
    public static List<ArchRule> all() {
        AllCentralRules.loadAll();
        List<ArchRule> rules = new ArrayList<>();
        for (Class<?> group : AllCentralRules.groups()) {
            rules.addAll(archRuleFieldsOf(group));
        }
        return List.copyOf(rules);
    }

    /** The {@code @ArchTest ArchRule} fields declared on a single group class. */
    public static List<ArchRule> archRuleFieldsOf(Class<?> group) {
        List<ArchRule> rules = new ArrayList<>();
        for (Field field : group.getDeclaredFields()) {
            if (isPublished(field, ArchRule.class)) {
                rules.add(read(field, ArchRule.class));
            }
        }
        return List.copyOf(rules);
    }

    /**
     * The {@code @ArchTest ArchTests} fields declared on an aggregator class — the only members
     * {@code ArchTests.in(aggregator)} actually descends into.
     */
    public static List<ArchTests> archTestsFieldsOf(Class<?> aggregator) {
        AllCentralRules.loadAll();
        List<ArchTests> nested = new ArrayList<>();
        for (Field field : aggregator.getDeclaredFields()) {
            if (isPublished(field, ArchTests.class)) {
                nested.add(read(field, ArchTests.class));
            }
        }
        return List.copyOf(nested);
    }

    /** Ids (= ArchUnit rule descriptions, = freeze-store keys) of every published rule. */
    public static List<String> ids() {
        return all().stream().map(ArchRule::getDescription).toList();
    }

    /** {@link #ids()} de-duplicated; use {@link #ids()} when duplicates are the thing under test. */
    public static Set<String> idSet() {
        return new LinkedHashSet<>(ids());
    }

    private static boolean isPublished(Field field, Class<?> type) {
        return field.isAnnotationPresent(ArchTest.class)
                && type.isAssignableFrom(field.getType())
                && Modifier.isStatic(field.getModifiers());
    }

    private static <T> T read(Field field, Class<T> type) {
        try {
            return type.cast(field.get(null));
        } catch (IllegalAccessException e) {
            throw new AssertionError("@ArchTest field must be public: " + field, e);
        }
    }
}
