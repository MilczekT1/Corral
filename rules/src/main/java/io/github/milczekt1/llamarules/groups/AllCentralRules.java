package io.github.milczekt1.llamarules.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import java.util.List;
import lombok.experimental.UtilityClass;

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
@UtilityClass
public class AllCentralRules {

    /** In documentation order. Kept in step with the {@code @ArchTest} fields below. */
    private static final List<Class<?>> MEMBERS = List.of(TestingRulesGroup.class);

    @ArchTest
    public static final ArchTests testing = ArchTests.in(TestingRulesGroup.class);

    public static List<Class<?>> members() {
        return MEMBERS;
    }
}
