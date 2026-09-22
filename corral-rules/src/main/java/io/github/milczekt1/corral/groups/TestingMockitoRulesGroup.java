package io.github.milczekt1.corral.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.corral.rules.testing.noconstructionmocking.NoConstructionMockingRule;
import io.github.milczekt1.corral.rules.testing.nostaticmocking.NoStaticMockingRule;
import lombok.experimental.UtilityClass;

/**
 * Testing rules that only mean something to a suite using Mockito.
 */
@UtilityClass
public class TestingMockitoRulesGroup {

    @ArchTest
    public static final ArchTests noStaticMocking =
            ArchTests.in(NoStaticMockingRule.class);

    @ArchTest
    public static final ArchTests noConstructionMocking =
            ArchTests.in(NoConstructionMockingRule.class);
}
