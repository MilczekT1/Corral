package io.github.milczekt1.llamaguard.reflect;

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
 * Reflection over what consumers actually evaluate: the {@code @ArchTest} fields reachable, however
 * deeply nested, from a given root class.
 *
 * <p>Assert against this rather than {@code RuleRegistry.all()}. The registry is process-wide and
 * Surefire reuses one JVM, so a sibling test registering a throwaway doc leaks into it, making any
 * assertion on its total contents order-dependent. What a walk from a root yields is
 * order-independent and pins exactly what a consumer running that root evaluates.
 *
 * <p><strong>Member resolution mirrors ArchUnit's.</strong> ArchUnit collects members with
 * {@code ReflectionUtils.getAllFields}, which streams {@code getDeclaredFields()} over every
 * supertype of the class — superclasses and interfaces alike — and reads each one after
 * {@code setAccessible(true)}. So this walk does the same: it looks up the whole supertype graph,
 * and it reads package-private and private fields, and fields of package-private classes. Being
 * narrower than ArchUnit would silently under-report, which is the dangerous direction: a rule that
 * consumers really do evaluate would drop out of every completeness assertion built on this class
 * without anything going red.
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
     * <p>The membership graph is assumed acyclic — a group that reaches itself, directly or through
     * another group, recurses until the stack overflows. That is deliberate: ArchUnit's own
     * {@code resolveArchRules} has no cycle guard either, so such a tree is already broken before
     * this walk ever sees it, and detecting it here would imply a robustness the framework does not
     * actually have.
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

    /**
     * The {@code @ArchTest ArchRule} fields visible on a class, whether declared on it or inherited
     * from a superclass or an interface.
     */
    public static List<ArchRule> archRuleFieldsOf(Class<?> owner) {
        List<ArchRule> rules = new ArrayList<>();
        for (Field field : publishedFieldsOf(owner, ArchRule.class)) {
            rules.add(read(field, ArchRule.class));
        }
        return List.copyOf(rules);
    }

    /**
     * The {@code @ArchTest ArchTests} fields visible on a class — the only members
     * {@code ArchTests.in(owner)} actually descends into. Inherited members count: ArchUnit
     * evaluates a group's members whether the group declares them or gets them from a base class or
     * an interface.
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
     * {@code root}.
     *
     * <p>Distinct because a rule may be reachable through more than one group. Encounter order is
     * preserved so output is stable, but neither {@code getDeclaredFields()} nor the order in which
     * supertypes are visited guarantees anything, so callers should not depend on it.
     */
    public static Set<String> idsOf(Class<?> root) {
        return rulesReachableFrom(root).stream()
                .map(ArchRule::getDescription)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * The published fields of the requested type across {@code owner} and every one of its
     * supertypes. Deduplicated by {@link Field}, whose equality is declaring class plus name, so an
     * interface reached along two paths of a diamond contributes its fields once.
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
        // null terminates the superclass chain (Object, primitives, interfaces); the set membership
        // check terminates the interface graph, which is a DAG and can revisit the same interface.
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
            // ArchUnit's ReflectionUtils.getValue does the same before reading, which is why a
            // package-private @ArchTest field, or one on a package-private class, is a shape it
            // supports; refusing to read it here would hide rules that consumers do evaluate.
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
