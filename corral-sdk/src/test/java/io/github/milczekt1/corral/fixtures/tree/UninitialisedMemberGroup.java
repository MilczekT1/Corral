package io.github.milczekt1.corral.fixtures.tree;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * The initialisation-order hazard {@code DocumentedRule}'s javadoc warns about, made concrete: an
 * {@code @ArchTest} field declared above the constant it reads, so it is left null once class
 * initialisation finishes.
 *
 * <p>The reference is qualified because a simple name here would be an illegal forward reference
 * and would not compile.
 */
public final class UninitialisedMemberGroup {

    @ArchTest
    public static final ArchRule declaredAboveWhatItReads = UninitialisedMemberGroup.DEFINITION;

    static final ArchRule DEFINITION = noClasses()
            .should().accessField(System.class, "in").as("fixture.uninitialised");
}
