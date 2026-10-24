package io.github.milczekt1.corral.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.corral.rules.exception.nogenericexceptionthrown.NoGenericExceptionThrownRule;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ExceptionRulesGroup {

    @ArchTest
    public static final ArchTests noGenericExceptionThrown = ArchTests.in(NoGenericExceptionThrownRule.class);
}
