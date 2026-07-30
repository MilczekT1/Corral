package io.github.milczekt1.archrules.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
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
 * evaluate) <strong>and</strong> an entry in {@link #groups()} (what the completeness and README
 * tooling reads). Registering only one of the two leaves the build green while nobody enforces the
 * new rules; {@code AllCentralRulesTest} fails if they diverge. The README growth path is the full
 * checklist.
 */
public final class AllCentralRules {

    /** Every seeded group class, in documentation order. Kept in step with the fields below. */
    private static final List<Class<?>> GROUPS = List.of(DatabaseRules.class, TestingRules.class);

    @ArchTest
    public static final ArchTests database = ArchTests.in(DatabaseRules.class);

    @ArchTest
    public static final ArchTests testing = ArchTests.in(TestingRules.class);

    private AllCentralRules() {
    }

    /** @see #GROUPS */
    public static List<Class<?>> groups() {
        return GROUPS;
    }

    /**
     * Forces static initialisation of every group class, populating
     * {@code RuleRegistry} with all their docs.
     *
     * <p>Needed because a class literal alone does not initialise a class — without this, tooling
     * that wants every doc up front (completeness checks, README generation) would see an empty
     * registry.
     */
    public static void loadAll() {
        for (Class<?> group : groups()) {
            try {
                Class.forName(group.getName(), true, group.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Could not load rule group " + group.getName(), e);
            }
        }
    }
}
