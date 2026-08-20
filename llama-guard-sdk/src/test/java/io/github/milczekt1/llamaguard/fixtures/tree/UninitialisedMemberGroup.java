package io.github.milczekt1.llamaguard.fixtures.tree;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * The initialisation-order hazard {@code DocumentedRule}'s javadoc warns about, made concrete: an
 * {@code @ArchTest} field declared above the constant it reads, so it is left null once class
 * initialisation finishes.
 *
 * <p>The reference is qualified because a simple name here would be an illegal forward reference and
 * would not compile — which is precisely why this mistake survives to runtime in real code, where
 * the constant usually sits in another class or is reached through a method call.
 */
public final class UninitialisedMemberGroup {

    @ArchTest
    public static final ArchRule declaredAboveWhatItReads = UninitialisedMemberGroup.RULE;

    static final ArchRule RULE = noClasses()
            .should().accessField(System.class, "in").as("fixture.uninitialised");
}
