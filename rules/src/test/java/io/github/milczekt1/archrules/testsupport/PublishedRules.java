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
 * <p>Assert against this rather than {@code RuleRegistry.all()}. The registry is process-wide and
 * Surefire reuses one JVM, so a sibling test registering a throwaway doc leaks into it, making any
 * assertion on its total contents order-dependent. The published fields are order-independent and
 * pin exactly what a consumer runs.
 *
 * <p>Test-only. Not named {@code *Test}, so Surefire skips it.
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
     * Every {@code @ArchTest ArchRule} reachable from {@code root}, descending through
     * {@code @ArchTest ArchTests} fields. Rule classes are leaves, groups are branches; one walk
     * handles both and any nesting.
     *
     * <p>{@code getDefinitionLocation()} is {@code @Internal}, but it is the only way to ask an
     * {@code ArchTests} which class it aggregates. Read-only and test-only, so an ArchUnit upgrade
     * that drops it costs a compile error here — never a wrong verdict in a consumer's build.
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
