package io.github.milczekt1.llamaguard.reflect;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.lang.ArchRule;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

/**
 * Reflection over what consumers actually evaluate: the {@code @ArchTest} fields reachable, however
 * deeply nested, from a given root class.
 *
 * <p>Assert against this rather than {@code RuleRegistry.all()}. The registry is process-wide and
 * Surefire reuses one JVM, so a sibling test registering a throwaway doc leaks into it, making any
 * assertion on its total contents order-dependent. What a walk from a root yields is
 * order-independent and pins exactly what a consumer running that root evaluates.
 *
 * <p><strong>Uses an ArchUnit internal.</strong> {@code ArchTests.getDefinitionLocation()} is
 * annotated {@code @Internal}, and it is the only way to ask an {@code ArchTests} which class it
 * aggregates. This class ships in the SDK jar, so an ArchUnit upgrade that drops the method breaks
 * the published artifact, not merely a local test — that is a real cost, accepted knowingly. What
 * limits it is that the call is read-only: the failure mode is a compile error, never a build that
 * passes while silently skipping rules.
 */
@UtilityClass
public class PublishedRules {

    /**
     * Every {@code @ArchTest ArchRule} reachable from {@code root}, descending through
     * {@code @ArchTest ArchTests} fields. Rule classes are leaves, groups are branches; one walk
     * handles both and any nesting.
     *
     * <p>Not deduplicated: a rule reachable through two groups appears twice, because the count of
     * evaluations is itself sometimes what a caller wants to check. Use {@link #idsOf(Class)} for
     * the distinct set.
     */
    public static List<ArchRule> rulesReachableFrom(Class<?> root) {
        List<ArchRule> collected = new ArrayList<>(archRuleFieldsOf(root));
        for (ArchTests nested : archTestsFieldsOf(root)) {
            collected.addAll(rulesReachableFrom(nested.getDefinitionLocation()));
        }
        return List.copyOf(collected);
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
        List<ArchTests> nested = new ArrayList<>();
        for (Field field : owner.getDeclaredFields()) {
            if (isPublished(field, ArchTests.class)) {
                nested.add(read(field, ArchTests.class));
            }
        }
        return List.copyOf(nested);
    }

    /**
     * The distinct ids (= ArchUnit rule descriptions, = freeze-store keys) reachable from
     * {@code root}.
     *
     * <p>Distinct because a rule may be reachable through more than one group. Encounter order is
     * preserved so output is stable, but {@code getDeclaredFields()} guarantees no order, so callers
     * should not depend on it.
     */
    public static Set<String> idsOf(Class<?> root) {
        return rulesReachableFrom(root).stream()
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
            throw new IllegalStateException("@ArchTest field must be public: " + field, e);
        }
    }
}
