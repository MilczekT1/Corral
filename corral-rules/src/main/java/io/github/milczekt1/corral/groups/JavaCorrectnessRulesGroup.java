package io.github.milczekt1.corral.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.corral.rules.java.nobigdecimaldoubleconstructor.NoBigDecimalDoubleConstructorRule;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JavaCorrectnessRulesGroup {

    @ArchTest
    public static final ArchTests noBigDecimalDoubleConstructor =
            ArchTests.in(NoBigDecimalDoubleConstructorRule.class);
}
