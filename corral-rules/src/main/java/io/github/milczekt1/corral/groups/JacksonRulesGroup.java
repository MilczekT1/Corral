package io.github.milczekt1.corral.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.corral.rules.jackson.staticobjectmappermustbefinal.StaticObjectMapperMustBeFinalRule;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JacksonRulesGroup {

    @ArchTest
    public static final ArchTests staticObjectMapperMustBeFinal = ArchTests.in(StaticObjectMapperMustBeFinalRule.class);
}
