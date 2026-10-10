package io.github.milczekt1.corral.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.corral.rules.testing.nolegacymockbeanandspybean.NoLegacyMockBeanAndSpyBeanRule;
import lombok.experimental.UtilityClass;

/**
 * Testing rules that only mean something to a suite using Spring's test support.
 *
 * <p>Requires Spring Boot 3.4 or later: on 3.3 and earlier, a new violation has no replacement to
 * migrate to. Do not wire this group there.
 */
@UtilityClass
public class TestingSpringRulesGroup {

    @ArchTest
    public static final ArchTests noLegacyMockBeanAndSpyBean =
            ArchTests.in(NoLegacyMockBeanAndSpyBeanRule.class);
}
