package io.github.milczekt1.corral.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.corral.rules.spring.nofieldinjection.NoFieldInjectionRule;
import io.github.milczekt1.corral.rules.spring.nosetterinjection.NoSetterInjectionRule;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SpringRulesGroup {

    @ArchTest
    public static final ArchTests noFieldInjection = ArchTests.in(NoFieldInjectionRule.class);

    @ArchTest
    public static final ArchTests noSetterInjection = ArchTests.in(NoSetterInjectionRule.class);
}
