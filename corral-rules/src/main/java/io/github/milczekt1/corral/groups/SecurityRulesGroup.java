package io.github.milczekt1.corral.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.corral.rules.security.nojdbcstatement.NoJdbcStatementRule;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SecurityRulesGroup {

    @ArchTest
    public static final ArchTests noJdbcStatement = ArchTests.in(NoJdbcStatementRule.class);
}
