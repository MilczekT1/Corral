package io.github.milczekt1.corral.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod.NoAsyncOnUnproxyableMethodRule;
import io.github.milczekt1.corral.rules.spring.nofieldinjection.NoFieldInjectionRule;
import io.github.milczekt1.corral.rules.spring.nosetterinjection.NoSetterInjectionRule;
import io.github.milczekt1.corral.rules.spring.notransactionalonfinalorstatic.NoTransactionalOnFinalOrStaticRule;
import io.github.milczekt1.corral.rules.spring.notransactionalonprivatemethod.NoTransactionalOnPrivateMethodRule;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SpringRulesGroup {

    @ArchTest
    public static final ArchTests noFieldInjection = ArchTests.in(NoFieldInjectionRule.class);

    @ArchTest
    public static final ArchTests noSetterInjection = ArchTests.in(NoSetterInjectionRule.class);

    @ArchTest
    public static final ArchTests noTransactionalOnPrivateMethod = ArchTests.in(NoTransactionalOnPrivateMethodRule.class);

    @ArchTest
    public static final ArchTests noTransactionalOnFinalOrStatic = ArchTests.in(NoTransactionalOnFinalOrStaticRule.class);

    @ArchTest
    public static final ArchTests noAsyncOnUnproxyableMethod = ArchTests.in(NoAsyncOnUnproxyableMethodRule.class);
}
