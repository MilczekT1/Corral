package io.github.milczekt1.corral.fixtures.tree;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.lang.ArchRule;

/**
 * The visibility shapes ArchUnit supports and the walk must therefore support: a member that is not
 * public, and a member on a class that is not public.
 *
 * <p>ArchUnit reads members through {@code setAccessible(true)}, and the consumer example in this
 * repo is the second shape: a package-private test class holding a package-private
 * {@code ArchTests} field.
 */
public final class RestrictedAccessGroup {

    /** Package-private member on a public class. Read from another package, so not accessible. */
    @ArchTest
    static final ArchRule packagePrivateMember = noClasses()
            .should().accessField(System.class, "in").as("fixture.restricted-field");

    @ArchTest
    public static final ArchTests membersOnAPackagePrivateClass = ArchTests.in(HiddenRules.class);

    /** Public member, package-private owner: it is the class that is out of reach, not the field. */
    static final class HiddenRules {

        @ArchTest
        public static final ArchRule onAPackagePrivateClass = noClasses()
                .should().accessField(System.class, "in").as("fixture.restricted-class");
    }
}
