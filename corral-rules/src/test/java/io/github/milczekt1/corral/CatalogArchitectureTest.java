package io.github.milczekt1.corral;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import io.github.milczekt1.corral.rules.testsupport.FixtureVerdicts;
import org.junit.jupiter.api.Test;

class CatalogArchitectureTest {

    private static final JavaClasses CATALOG = new ClassFileImporter().importPackages("io.github.milczekt1.corral");
    private static final JavaClasses PUBLISHED_RULES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.github.milczekt1.corral.rules");

    /** Main and test classes alike, so one rule's test borrowing another rule's fixtures fails too. */
    @Test
    void rulesDoNotDependOnEachOther() {
        slices().matching("io.github.milczekt1.corral.rules.(*).(*)..")
                .should().notDependOnEachOther()
                .check(CATALOG);
    }

    @Test
    void everyRuleTestChecksItsFixtureVerdictsAndCommittedStore() {
        classes().that().haveSimpleNameEndingWith("RuleTest")
                .should().callMethod(FixtureVerdicts.class, "assertMatch", Class.class, ArchRule.class, JavaClasses.class)
                .andShould().callMethod(CommittedStore.class, "freeze", ArchRule.class, String.class, JavaClasses.class)
                .check(CATALOG);
    }

    @Test
    void ruleTestsDeclareNoNestedClasses() {
        noClasses().that().areNestedClasses()
                .should().haveNameMatching(".*RuleTest\\$.*")
                .because("Sonar lints a class nested in a test as a real test; an example belongs in fixtures/")
                .check(CATALOG);
    }

    @Test
    void ruleClassesAreFinalDocumentedRulesWithPrivateConstructors() {
        classes().that().areTopLevelClasses().and().haveSimpleNameEndingWith("Rule")
                .should().haveModifier(JavaModifier.FINAL)
                .andShould().implement(DocumentedRule.class)
                .andShould().haveOnlyPrivateConstructors()
                .check(PUBLISHED_RULES);
    }

    @Test
    void ruleDocsAndDefinitionsAreStaticFinalAndPackagePrivate() {
        fields().that().haveName("DOC")
                .should().bePackagePrivate().andShould().beStatic().andShould().beFinal()
                .andShould().haveRawType(RuleDoc.class)
                .check(PUBLISHED_RULES);
        fields().that().haveName("DEFINITION")
                .should().bePackagePrivate().andShould().beStatic().andShould().beFinal()
                .andShould().haveRawType(ArchRule.class)
                .check(PUBLISHED_RULES);
    }

    @Test
    void publishedRuleFieldsArePublicStaticFinalArchTests() {
        fields().that().haveName("rule")
                .should().bePublic().andShould().beStatic().andShould().beFinal()
                .andShould().haveRawType(ArchRule.class)
                .andShould().beAnnotatedWith(ArchTest.class)
                .check(PUBLISHED_RULES);
    }
}
