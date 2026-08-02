package io.github.milczekt1.archrules.groups;

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
 * <p>Growth path — add a group class here once it is seeded:
 * {@code Java17Rules}, {@code JakartaMigrationRules}, {@code SpringRules}. Every group needs
 * <strong>both</strong> an {@code @ArchTest ArchTests} field (the only members
 * {@code ArchTests.in(AllCentralRules.class)} descends into, so this is what consumers actually
 * evaluate) <strong>and</strong> an entry in {@link #members()} (what the completeness and README
 * tooling reads). Registering only one of the two leaves the build green while nobody enforces the
 * new rules; {@code GroupMembershipTest} walks the whole tree from here and fails if they diverge,
 * at this level or any level below. The README growth path is the full checklist.
 *
 * <p>A member listed here may itself be a group (something that in turn declares its own
 * {@code @ArchTest ArchTests} fields) or a rule class (something that declares {@code @ArchTest
 * ArchRule} fields directly). {@link #loadAll()} handles either shape, and any nesting of them.
 */
public final class AllCentralRules {

    /** Every seeded member class, in documentation order. Kept in step with the fields below. */
    private static final List<Class<?>> MEMBERS = List.of(TestingRules.class);

    @ArchTest
    public static final ArchTests testing = ArchTests.in(TestingRules.class);

    private AllCentralRules() {
    }

    /** @see #MEMBERS */
    public static List<Class<?>> members() {
        return MEMBERS;
    }

    /**
     * Forces static initialisation of every member reachable from {@link #MEMBERS}, however many
     * {@code @ArchTest ArchTests} levels deep it actually sits, populating {@code RuleRegistry}
     * with every doc along the way.
     *
     * <p>Needed because a class literal alone does not initialise a class — without this, tooling
     * that wants every doc up front (completeness checks, README generation) would see an empty
     * registry.
     */
    public static void loadAll() {
        loadAll(MEMBERS);
    }

    /**
     * Package-private so {@code RuleRegistryCompletenessTest} can exercise the recursion directly
     * against a throwaway fixture, independent of the real {@link #MEMBERS}.
     *
     * <p>Recurses explicitly rather than trusting a group's own static initialisation to cascade
     * into what its {@code ArchTests} field points at. It does not: decompiling {@code ArchTests}
     * shows {@code ArchTests.in(X.class)} does nothing but store the {@code Class} object on the
     * new instance, and a direct experiment confirms the consequence — {@code Class.forName} on a
     * class that merely holds a {@code Class} reference to another class does not initialise that
     * other class (a class literal, and a field that stores one, are not JLS class-initialisation
     * triggers). So loading a group alone does not load what its {@code ArchTests} field points at,
     * and this method walks down explicitly instead of assuming it does.
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
     * The classes an already-loaded member itself points at via its own {@code @ArchTest
     * ArchTests} fields. Empty for a rule class (which declares {@code ArchRule} fields instead),
     * which is how the recursion above terminates at the leaves.
     *
     * <p>{@code ArchTests.getDefinitionLocation()} is annotated {@code @Internal}, so it is not
     * part of ArchUnit's public API. Reading it here is deliberate: it is the only way to ask an
     * {@code ArchTests} field which class it actually aggregates, and that question is exactly
     * what this recursion needs answered at every level. It is read-only, so the worst case of
     * ArchUnit removing it is a compile error in this module the next time its ArchUnit dependency
     * is bumped — never a wrong verdict in a consumer's build.
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
