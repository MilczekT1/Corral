package io.github.milczekt1.corral.reflect;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.lang.ArchRule;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

/**
 * Reflection over the {@code @ArchTest} fields reachable, however deeply nested, from a root class.
 *
 * <p>Prefer this to {@code RuleRegistry.all()} in assertions: the registry is process-wide, so in a
 * reused JVM a sibling test's registration leaks into it.
 *
 * <p>Member resolution mirrors ArchUnit's {@code ReflectionUtils.getAllFields}: the whole supertype
 * graph, reading private and package-private fields alike.
 *
 * <p>{@code ArchTests.getDefinitionLocation()} is annotated {@code @Internal} and this class ships
 * in the SDK jar, so an ArchUnit upgrade that drops it breaks the published artifact.
 */
@UtilityClass
public class PublishedRules {

    /**
     * Every {@code @ArchTest ArchRule} reachable from {@code root}, descending through
     * {@code @ArchTest ArchTests} fields.
     *
     * <p>Not deduplicated — a rule reachable through two groups appears twice; see
     * {@link #idsOf(Class)}. A cyclic membership graph recurses until the stack overflows, as it
     * does in ArchUnit's own {@code resolveArchRules}.
     */
    public static List<ArchRule> rulesReachableFrom(Class<?> root) {
        List<ArchRule> collected = new ArrayList<>(archRuleFieldsOf(root));
        for (ArchTests nested : archTestsFieldsOf(root)) {
            collected.addAll(rulesReachableFrom(nested.getDefinitionLocation()));
        }
        return List.copyOf(collected);
    }

    /** The {@code @ArchTest ArchRule} fields visible on a class, declared or inherited. */
    public static List<ArchRule> archRuleFieldsOf(Class<?> owner) {
        List<ArchRule> rules = new ArrayList<>();
        for (Field field : publishedFieldsOf(owner, ArchRule.class)) {
            rules.add(read(field, ArchRule.class));
        }
        return List.copyOf(rules);
    }

    /**
     * The {@code @ArchTest ArchTests} fields visible on a class, declared or inherited — the only
     * members {@code ArchTests.in(owner)} descends into.
     */
    public static List<ArchTests> archTestsFieldsOf(Class<?> owner) {
        List<ArchTests> nested = new ArrayList<>();
        for (Field field : publishedFieldsOf(owner, ArchTests.class)) {
            nested.add(read(field, ArchTests.class));
        }
        return List.copyOf(nested);
    }

    /**
     * The distinct ids (= ArchUnit rule descriptions, = freeze-store keys) reachable from
     * {@code root}. Encounter order is preserved but is not guaranteed by reflection.
     */
    public static Set<String> idsOf(Class<?> root) {
        return rulesReachableFrom(root).stream()
                .map(ArchRule::getDescription)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * The published fields of the requested type across {@code owner} and every supertype.
     * Deduplicated by {@link Field} — declaring class plus name — so a diamond contributes once.
     */
    private static List<Field> publishedFieldsOf(Class<?> owner, Class<?> type) {
        Set<Field> fields = new LinkedHashSet<>();
        for (Class<?> supertype : selfAndAllSupertypesOf(owner)) {
            for (Field field : supertype.getDeclaredFields()) {
                if (isPublished(field, type)) {
                    fields.add(field);
                }
            }
        }
        return List.copyOf(fields);
    }

    /** {@code type} plus every superclass and every interface, transitively. */
    private static Set<Class<?>> selfAndAllSupertypesOf(Class<?> type) {
        Set<Class<?>> supertypes = new LinkedHashSet<>();
        collectSupertypes(type, supertypes);
        return supertypes;
    }

    private static void collectSupertypes(Class<?> type, Set<Class<?>> into) {
        // The set membership check terminates the interface graph, which can revisit an interface.
        if (type == null || !into.add(type)) {
            return;
        }
        collectSupertypes(type.getSuperclass(), into);
        for (Class<?> implemented : type.getInterfaces()) {
            collectSupertypes(implemented, into);
        }
    }

    private static boolean isPublished(Field field, Class<?> type) {
        return field.isAnnotationPresent(ArchTest.class)
                && type.isAssignableFrom(field.getType())
                && Modifier.isStatic(field.getModifiers());
    }

    private static <T> T read(Field field, Class<T> type) {
        try {
            // ArchUnit's ReflectionUtils.getValue does the same, so package-private @ArchTest
            // fields are a shape it supports.
            field.setAccessible(true);
            Object value = field.get(null);
            if (value == null) {
                throw new IllegalStateException("@ArchTest field is still null: " + field
                        + " — a static field initialised from constants declared below it reads them"
                        + " before they exist. Declare the @ArchTest field after the constants it"
                        + " reads.");
            }
            return type.cast(value);
        } catch (IllegalAccessException | InaccessibleObjectException e) {
            throw new IllegalStateException("cannot read @ArchTest field " + field
                    + " — its module does not open the declaring package for reflection. Open the"
                    + " package to this module, or move the rule to a package that is open.", e);
        }
    }
}
