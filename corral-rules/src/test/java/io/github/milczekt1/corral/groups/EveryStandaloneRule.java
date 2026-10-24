package io.github.milczekt1.corral.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.corral.rules.concurrency.nofuturegetwithouttimeout.NoFutureGetWithoutTimeoutRule;
import io.github.milczekt1.corral.rules.dependencies.nojavax.NoJavaxRule;
import io.github.milczekt1.corral.rules.jackson.noannotationsonentity.NoAnnotationsOnEntityRule;
import io.github.milczekt1.corral.rules.java.nointernalapi.NoInternalApiRule;
import io.github.milczekt1.corral.rules.java.nolegacydateapi.NoLegacyDateApiRule;
import io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections.NoLegacySynchronizedCollectionsRule;
import io.github.milczekt1.corral.rules.java.nooptionalfields.NoOptionalFieldsRule;
import io.github.milczekt1.corral.rules.java.nooptionalmethodparameters.NoOptionalMethodParametersRule;
import io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.SerializableMustDeclareSerialVersionUidRule;
import io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.NoTestLibrariesInProductionRule;
import io.github.milczekt1.corral.rules.naming.lowercasepackages.LowercasePackagesRule;
import io.github.milczekt1.corral.rules.spring.norepositoryincontroller.NoRepositoryInControllerRule;
import io.github.milczekt1.corral.rules.spring.notransactionalincontroller.NoTransactionalInControllerRule;
import io.github.milczekt1.corral.rules.spring.novalueonfield.NoValueOnFieldRule;
import io.github.milczekt1.corral.rules.testing.nolegacymockbeanandspybean.NoLegacyMockBeanAndSpyBeanRule;
import io.github.milczekt1.corral.rules.testing.nomutablestaticstate.NoMutableStaticStateRule;
import lombok.experimental.UtilityClass;

/**
 * Every rule the catalog ships outside any group, as one root the invariant tests here can walk.
 *
 * <p>A rule stands alone rather than joining a group when it is sound but not right for every
 * codebase: a group is adopted whole, so a rule inside one runs for everybody who wires that group.
 * Consumers reach a standalone rule by naming its class — {@code ArchTests.in(TheRule.class)} —
 * which is exactly what a group's field does, so nothing about the rule itself differs.
 *
 * <p>Test-scoped, like {@link EveryPublishedGroup}, and listed for the same reason: without it the
 * grammar, doc and id-uniqueness checks would never see these rules. Nothing fails when a rule is
 * added to the catalog and not listed below — {@code RulesCatalogDocTest} only catches it once the
 * rule has a row in {@code docs/rules.md}.
 */
@UtilityClass
public class EveryStandaloneRule {

    @ArchTest
    public static final ArchTests noMutableStaticState = ArchTests.in(NoMutableStaticStateRule.class);

    @ArchTest
    public static final ArchTests noJavax = ArchTests.in(NoJavaxRule.class);

    @ArchTest
    public static final ArchTests noInternalApi = ArchTests.in(NoInternalApiRule.class);

    @ArchTest
    public static final ArchTests noLegacyDateApi = ArchTests.in(NoLegacyDateApiRule.class);

    @ArchTest
    public static final ArchTests noLegacySynchronizedCollections =
            ArchTests.in(NoLegacySynchronizedCollectionsRule.class);

    @ArchTest
    public static final ArchTests noOptionalFields = ArchTests.in(NoOptionalFieldsRule.class);

    @ArchTest
    public static final ArchTests noOptionalMethodParameters =
            ArchTests.in(NoOptionalMethodParametersRule.class);

    @ArchTest
    public static final ArchTests serializableMustDeclareSerialVersionUid =
            ArchTests.in(SerializableMustDeclareSerialVersionUidRule.class);

    @ArchTest
    public static final ArchTests noLegacyMockBeanAndSpyBean =
            ArchTests.in(NoLegacyMockBeanAndSpyBeanRule.class);

    @ArchTest
    public static final ArchTests noValueOnField = ArchTests.in(NoValueOnFieldRule.class);

    @ArchTest
    public static final ArchTests noRepositoryInController = ArchTests.in(NoRepositoryInControllerRule.class);

    @ArchTest
    public static final ArchTests noTransactionalInController =
            ArchTests.in(NoTransactionalInControllerRule.class);

    @ArchTest
    public static final ArchTests noFutureGetWithoutTimeout =
            ArchTests.in(NoFutureGetWithoutTimeoutRule.class);

    @ArchTest
    public static final ArchTests lowercasePackages = ArchTests.in(LowercasePackagesRule.class);

    @ArchTest
    public static final ArchTests noJacksonAnnotationsOnEntity = ArchTests.in(NoAnnotationsOnEntityRule.class);

    @ArchTest
    public static final ArchTests noTestLibrariesInProduction =
            ArchTests.in(NoTestLibrariesInProductionRule.class);
}
