package io.github.milczekt1.corral.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.PersistentClassMustHaveNoArgConstructorRule;
import lombok.experimental.UtilityClass;

/**
 * Rules for what the Persistence specification requires of entity and embeddable classes,
 * {@code jakarta} and {@code javax} alike.
 */
@UtilityClass
public class JakartaPersistenceRulesGroup {

    @ArchTest
    public static final ArchTests persistentClassMustHaveNoArgConstructor =
            ArchTests.in(PersistentClassMustHaveNoArgConstructorRule.class);
}
