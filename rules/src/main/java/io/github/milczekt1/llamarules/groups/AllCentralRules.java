package io.github.milczekt1.llamarules.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Opt into every central rule group with a single field:
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "com.acme", importOptions = ImportOption.DoNotIncludeJars.class)
 * class CentralArchitectureTest {
 *     @ArchTest
 *     static final ArchTests all = ArchTests.in(AllCentralRules.class);
 * }
 * }</pre>
 *
 * <p>Every member needs <strong>both</strong> an {@code @ArchTest ArchTests} field (what consumers
 * evaluate) and an entry in {@link #members()} (what tooling reads). Register only one and the build
 * stays green while nobody enforces the rules; {@code GroupMembershipTest} fails on that divergence
 * at any depth.
 *
 * <p>A member may be another group or a rule class. See the README for the full growth path.
 */
public final class AllCentralRules {

    /** In documentation order. Kept in step with the {@code @ArchTest} fields below. */
    private static final List<Class<?>> MEMBERS = List.of(TestingRulesGroup.class);

    @ArchTest
    public static final ArchTests testing = ArchTests.in(TestingRulesGroup.class);

    private AllCentralRules() {
    }

    public static List<Class<?>> members() {
        return MEMBERS;
    }

    /**
     * Initialises every member at any depth, so {@code RuleRegistry} holds every doc. Tooling that
     * needs all docs up front would otherwise see an empty registry, because a class literal does
     * not initialise the class it names.
     */
    public static void loadAll() {
        loadAll(MEMBERS);
    }

    /**
     * Package-private so tests can drive the recursion against a fixture instead of {@link #MEMBERS}.
     *
     * <p>Descends explicitly. {@code ArchTests.in(X)} only stores {@code X}, and storing a class
     * literal is not a JLS initialisation trigger, so loading a group does <em>not</em> load what
     * its {@code ArchTests} fields point at.
     */
    static void loadAll(List<Class<?>> members) {
        for (Class<?> member : members) {
            Class<?> loaded = load(member);
            loadAll(nestedMembersOf(loaded));
        }
    }

    private static Class<?> load(Class<?> member) {
        try {
            return Class.forName(member.getName(), true, member.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Could not load rule member " + member.getName(), e);
        }
    }

    /**
     * What a loaded member points at through its own {@code @ArchTest ArchTests} fields. Empty for a
     * rule class, which is how the recursion terminates.
     *
     * <p>{@code getDefinitionLocation()} is {@code @Internal}, but it is the only way to ask an
     * {@code ArchTests} which class it aggregates. Read-only, so an ArchUnit upgrade that drops it
     * costs a compile error here — never a wrong verdict in a consumer's build.
     */
    private static List<Class<?>> nestedMembersOf(Class<?> loaded) {
        List<Class<?>> nested = new ArrayList<>();
        for (Field field : loaded.getDeclaredFields()) {
            if (field.isAnnotationPresent(ArchTest.class)
                    && ArchTests.class.isAssignableFrom(field.getType())
                    && Modifier.isStatic(field.getModifiers())) {
                nested.add(readArchTests(field).getDefinitionLocation());
            }
        }
        return nested;
    }

    private static ArchTests readArchTests(Field field) {
        try {
            return (ArchTests) field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not read @ArchTest ArchTests field " + field, e);
        }
    }
}
