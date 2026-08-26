package io.github.milczekt1.corral.rules.testing;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.scope.TestScope;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Top-level test classes must be named so a build tool's convention actually selects them.
 *
 * <p>Inspects <em>test</em> classes, so consumers must not set
 * {@code ImportOption.DoNotIncludeTests} — it would pass vacuously.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestClassNamingConventionRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.test.class-names-must-end-with-test-or-it")
            .why("""
                    Most build tools select which top-level classes to run by class-name convention (Maven's \
                    Surefire and Failsafe plugins, for example, match *Test and *IT respectively). A \
                    top-level class holding JUnit test methods — @Test, @ParameterizedTest, @RepeatedTest, \
                    @TestFactory or @TestTemplate — whose name ends in none of Test, Tests or IT is silently \
                    never executed: it looks like coverage in the source tree while proving nothing in CI.""")
            .howToFix("""
                    Rename the reported top-level class to end in Test or Tests (unit tests) or IT (integration \
                    tests) so your build tool's test-selection convention picks it up (with Maven, Surefire \
                    runs *Test and Failsafe runs *IT). Nested classes are never reported by this rule and \
                    must not be renamed: a JUnit 5 @Nested group is executed through its enclosing class, \
                    whose name is the only one the build tool ever looks at.""")
            .howNotToFix("""
                    Do NOT delete the test methods or the class to make this rule pass, and do NOT \
                    widen your build tool's test-include configuration instead of renaming (for example, \
                    Surefire's include patterns) — the convention is what makes the unit/integration split \
                    legible. Do NOT swap @Test for @ParameterizedTest or any other JUnit test annotation \
                    either; every one of them counts.""")
            .build();

    /**
     * The three JUnit 5 roots this rule selects on, taken from the SDK so there is one definition
     * of what JUnit executes — see {@link TestScope#JUNIT_TEST_ANNOTATIONS}.
     *
     * <p>Kept as a field here because this rule's own test pins it, so an SDK-side edit surfaces as
     * a failure rather than as a silent change in which classes get reported.
     */
    static final List<String> JUNIT_TEST_ANNOTATIONS = TestScope.JUNIT_TEST_ANNOTATIONS;

    /**
     * {@link TestScope#isJUnitTestMethod} and <strong>not</strong> {@code TestScope.TEST_CLASSES}:
     * the class-level predicate is {@code location OR structure}, so it also holds for fixtures and
     * helpers that declare no test — a different rule under the same id.
     *
     * <p>The description text is this rule's own and stays byte-identical to what shipped: it is
     * rendered into the rule's description, which is the freeze-store matching key.
     */
    static final ArchRule DEFINITION = classes()
            .that().containAnyMethodsThat(describe("annotated with a JUnit 5 test annotation",
                    TestScope::isJUnitTestMethod))
            .and().areNotMemberClasses()
            .should().haveSimpleNameEndingWith("Test")
            .orShould().haveSimpleNameEndingWith("Tests")
            .orShould().haveSimpleNameEndingWith("IT");

    @ArchTest
    public static final ArchRule rule = new TestClassNamingConventionRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
