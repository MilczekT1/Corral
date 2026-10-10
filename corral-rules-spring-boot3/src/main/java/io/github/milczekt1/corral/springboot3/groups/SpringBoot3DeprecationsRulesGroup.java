package io.github.milczekt1.corral.springboot3.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.corral.springboot3.rules.testing.nolegacymockbeanandspybean.NoLegacyMockBeanAndSpyBeanRule;
import lombok.experimental.UtilityClass;

/**
 * Rules against Spring Boot 3 APIs that Boot 4 removes.
 *
 * <p>For Spring Boot 3.4 to 3.x only. On 3.3 and earlier a violation has no replacement to migrate
 * to; on Boot 4 the APIs are gone and these rules can never fire. On the Boot 4 upgrade, remove the
 * {@code corral-rules-spring-boot3} dependency, the field wiring this group, and the
 * {@code archunit/frozen} entries for its rule ids.
 */
@UtilityClass
public class SpringBoot3DeprecationsRulesGroup {

    @ArchTest
    public static final ArchTests noLegacyMockBeanAndSpyBean =
            ArchTests.in(NoLegacyMockBeanAndSpyBeanRule.class);
}
