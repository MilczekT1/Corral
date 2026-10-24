package io.github.milczekt1.corral.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.corral.rules.java.nobigdecimaldoubleconstructor.NoBigDecimalDoubleConstructorRule;
import io.github.milczekt1.corral.rules.java.noclassnewinstance.NoClassNewInstanceRule;
import io.github.milczekt1.corral.rules.java.nofinalizeoverride.NoFinalizeOverrideRule;
import io.github.milczekt1.corral.rules.java.nopublicstaticarrayfields.NoPublicStaticArrayFieldsRule;
import io.github.milczekt1.corral.rules.java.publicstaticfieldsmustbefinal.PublicStaticFieldsMustBeFinalRule;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JavaCorrectnessRulesGroup {

    @ArchTest
    public static final ArchTests noBigDecimalDoubleConstructor =
            ArchTests.in(NoBigDecimalDoubleConstructorRule.class);

    @ArchTest
    public static final ArchTests publicStaticFieldsMustBeFinal =
            ArchTests.in(PublicStaticFieldsMustBeFinalRule.class);

    @ArchTest
    public static final ArchTests noPublicStaticArrayFields =
            ArchTests.in(NoPublicStaticArrayFieldsRule.class);

    @ArchTest
    public static final ArchTests noClassNewInstance = ArchTests.in(NoClassNewInstanceRule.class);

    @ArchTest
    public static final ArchTests noFinalizeOverride = ArchTests.in(NoFinalizeOverrideRule.class);
}
