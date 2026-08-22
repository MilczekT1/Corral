package io.github.milczekt1.corral.fixtures.tree;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Members contributed by a superclass — a shared base a family of groups extends to pick up a
 * common baseline. ArchUnit evaluates these for every subclass, so the walk must see them there too.
 *
 * <p>Declares one of each kind: a leaf rule and a nested group, so a subclass exercises both descent
 * paths through inheritance rather than only the flat one.
 */
public abstract class InheritedMembersBaseGroup {

    @ArchTest
    public static final ArchRule declaredOnASuperclass = noClasses()
            .should().accessField(System.class, "in").as("fixture.inherited-from-superclass");

    @ArchTest
    public static final ArchTests nestedGroupOnASuperclass = ArchTests.in(BetaFixtureRule.class);
}
