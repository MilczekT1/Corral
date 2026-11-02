package io.github.milczekt1.corral.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.corral.rules.logging.noprintstacktrace.NoPrintStackTraceRule;
import io.github.milczekt1.corral.rules.logging.nosystemerr.NoSystemErrRule;
import io.github.milczekt1.corral.rules.logging.nosystemout.NoSystemOutRule;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LoggingRulesGroup {

    @ArchTest
    public static final ArchTests noSystemOut = ArchTests.in(NoSystemOutRule.class);

    @ArchTest
    public static final ArchTests noSystemErr = ArchTests.in(NoSystemErrRule.class);

    @ArchTest
    public static final ArchTests noPrintStackTrace = ArchTests.in(NoPrintStackTraceRule.class);
}
