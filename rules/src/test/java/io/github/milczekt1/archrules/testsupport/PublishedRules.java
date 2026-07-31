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
import java.util.stream.Collectors;

/**
 * Reflection over what consumers actually evaluate: the {@code @ArchTest} fields reachable,
 * however deeply nested, from {@link AllCentralRules}.
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

    /** Every {@code @ArchTest ArchRule} reachable from {@link AllCentralRules}, in group order. */
    public static List<ArchRule> all() {
        AllCentralRules.loadAll();
        return rulesReachableFrom(AllCentralRules.class);
    }

    /**
     * Every {@code @ArchTest ArchRule} reachable from {@code root}, descending through any
     * {@code @ArchTest ArchTests} fields. A rule class is a leaf (it declares ArchRule fields); a
     * group is a branch (it declares ArchTests fields). Both shapes, and any nesting of them, are
     * handled by the same walk.
     *
     * <p>{@code ArchTests.getDefinitionLocation()} is annotated {@code @Internal}, so it is not
     * part of ArchUnit's public API. Reading it here is deliberate and confined to this test-only
     * class: it is the only way to ask an {@code ArchTests} field which class it actually
     * aggregates, and that question is exactly what this walk needs answered at every level. It is
     * read-only, so the worst case of ArchUnit removing it is a compile error in this test module —
     * never a wrong verdict in a consumer's build.
     */
    public static List<ArchRule> rulesReachableFrom(Class<?> root) {
        List<ArchRule> collected = new ArrayList<>(archRuleFieldsOf(root));
        for (ArchTests nested : archTestsFieldsOf(root)) {
            collected.addAll(rulesReachableFrom(nested.getDefinitionLocation()));
        }
        return collected;
    }

    /** The {@code @ArchTest ArchRule} fields declared on a single class. */
    public static List<ArchRule> archRuleFieldsOf(Class<?> owner) {
        List<ArchRule> rules = new ArrayList<>();
        for (Field field : owner.getDeclaredFields()) {
            if (isPublished(field, ArchRule.class)) {
                rules.add(read(field, ArchRule.class));
            }
        }
        return List.copyOf(rules);
    }

    /**
     * The {@code @ArchTest ArchTests} fields declared on a single class — the only members
     * {@code ArchTests.in(owner)} actually descends into.
     */
    public static List<ArchTests> archTestsFieldsOf(Class<?> owner) {
        AllCentralRules.loadAll();
        List<ArchTests> nested = new ArrayList<>();
        for (Field field : owner.getDeclaredFields()) {
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

    /**
     * {@link #ids()} de-duplicated; use {@link #ids()} when duplicates are the thing under test.
     *
     * <p>Starts from {@link AllCentralRules} and walks {@link #rulesReachableFrom(Class)}, so it
     * reflects whatever nesting the group tree actually has, rather than assuming one level.
     */
    public static Set<String> idSet() {
        AllCentralRules.loadAll();
        return rulesReachableFrom(AllCentralRules.class).stream()
                .map(ArchRule::getDescription)
                .collect(Collectors.toCollection(LinkedHashSet::new));
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
