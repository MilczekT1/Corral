package io.github.milczekt1.corral.fixtures.tree;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * A member contributed by an interface. Interface fields are implicitly {@code public static final},
 * and ArchUnit evaluates this rule for anything implementing the interface.
 *
 * <p>Raw {@code ArchRule}s rather than {@code DocumentedRule}s, as elsewhere in this tree: nothing
 * here should reach the process-wide {@code RuleRegistry}.
 */
public interface InterfaceDeclaredRules {

    @ArchTest
    ArchRule declaredOnAnInterface = noClasses()
            .should().accessField(System.class, "in").as("fixture.inherited-from-interface");
}
