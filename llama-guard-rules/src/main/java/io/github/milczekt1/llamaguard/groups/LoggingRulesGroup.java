package io.github.milczekt1.llamaguard.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.llamaguard.rules.logging.NoSystemErrRule;
import io.github.milczekt1.llamaguard.rules.logging.NoSystemOutRule;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LoggingRulesGroup {

    @ArchTest
    public static final ArchTests noSystemOut = ArchTests.in(NoSystemOutRule.class);

    @ArchTest
    public static final ArchTests noSystemErr = ArchTests.in(NoSystemErrRule.class);
}
