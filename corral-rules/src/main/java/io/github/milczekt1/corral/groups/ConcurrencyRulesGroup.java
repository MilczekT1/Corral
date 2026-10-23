package io.github.milczekt1.corral.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.corral.rules.concurrency.nofuturegetwithouttimeout.NoFutureGetWithoutTimeoutRule;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ConcurrencyRulesGroup {

    @ArchTest
    public static final ArchTests noFutureGetWithoutTimeout =
            ArchTests.in(NoFutureGetWithoutTimeoutRule.class);
}
