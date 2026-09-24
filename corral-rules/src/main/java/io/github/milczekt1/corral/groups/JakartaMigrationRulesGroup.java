package io.github.milczekt1.corral.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.corral.rules.jakarta.nojavaxvalidation.NoJavaxValidationRule;
import lombok.experimental.UtilityClass;

/**
 * Rules that forbid a {@code javax} namespace Jakarta EE 9 renamed to {@code jakarta}.
 */
@UtilityClass
public class JakartaMigrationRulesGroup {

    @ArchTest
    public static final ArchTests noJavaxValidation =
            ArchTests.in(NoJavaxValidationRule.class);
}
