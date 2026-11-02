package io.github.milczekt1.corral;

import static com.tngtech.archunit.lang.conditions.ArchConditions.be;
import static com.tngtech.archunit.lang.conditions.ArchConditions.have;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SliceAssignment;
import com.tngtech.archunit.library.dependencies.SliceIdentifier;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import io.github.milczekt1.corral.rules.testsupport.FixtureVerdicts;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CatalogArchitectureTest {

    private static final String RULES = "io.github.milczekt1.corral.rules";
    private static final JavaClasses CATALOG =new ClassFileImporter().importPackages("io.github.milczekt1.corral");
    /** Everything this module publishes; Surefire runs with the module directory as working directory. */
    private static final JavaClasses PUBLISHED = new ClassFileImporter().importPath(Path.of("target/classes"));

    /** Main and test classes alike, so one rule's test borrowing another rule's fixtures fails too. */
    @Test
    void rulesDoNotDependOnEachOther() {
        slices().assignedFrom(new RulePackages())
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
    void everyRuleIsNamedAsOneAndTestedInItsPackage() {
        classes().that().implement(DocumentedRule.class)
                .should().haveSimpleNameEndingWith("Rule")
                .andShould().resideInAPackage(RULES + "..")
                .andShould(have(DescribedPredicate.describe("a test in its package",
                        rule -> CATALOG.contain(rule.getName() + "Test"))))
                .check(PUBLISHED);
    }

    @Test
    void everyClassUnderRulesIsARuleAloneInItsPackage() {
        classes().that().resideInAPackage(RULES + "..").and().areTopLevelClasses()
                .should().implement(DocumentedRule.class)
                .andShould(be(DescribedPredicate.describe("the only class in its package",
                        javaClass -> javaClass.getPackage().getClasses().stream()
                                .filter(JavaClass::isTopLevelClass).count() == 1)))
                .check(PUBLISHED);
    }

    /** A rule's slice is its package and every subpackage, down to the next rule's package. */
    private static final class RulePackages implements SliceAssignment {

        private final Set<String> rulePackages = PUBLISHED.stream()
                .filter(javaClass -> javaClass.isAssignableTo(DocumentedRule.class))
                .map(JavaClass::getPackageName)
                .collect(Collectors.toSet());

        @Override
        public SliceIdentifier getIdentifierOf(JavaClass javaClass) {
            for (String pkg = javaClass.getPackageName(); !pkg.isEmpty(); pkg = parentOf(pkg)) {
                if (rulePackages.contains(pkg)) {
                    return SliceIdentifier.of(pkg);
                }
            }
            return SliceIdentifier.ignore();
        }

        @Override
        public String getDescription() {
            return "rule packages";
        }

        private static String parentOf(String pkg) {
            return pkg.contains(".") ? pkg.substring(0, pkg.lastIndexOf('.')) : "";
        }
    }
}
