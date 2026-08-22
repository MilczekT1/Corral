package io.github.milczekt1.corral.rules.testing;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
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
            .id("test.class-naming-convention")
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
     * The three JUnit 5 roots, matched by FQN string so a consumer without
     * {@code junit-jupiter-params} still works.
     *
     * <p>Only roots are listed because the predicate below also matches meta-annotations:
     * {@code @ParameterizedTest} and {@code @RepeatedTest} are themselves annotated with
     * {@code @TestTemplate} and are reached through it, as is any project's own composed annotation.
     * Enumerating the leaves instead would mean this rule silently stops covering whatever JUnit or
     * the consumer adds next — the exact false negative it exists to catch.
     */
    static final List<String> JUNIT_TEST_ANNOTATIONS = List.of(
            "org.junit.jupiter.api.Test",
            "org.junit.jupiter.api.TestFactory",
            "org.junit.jupiter.api.TestTemplate");

    /**
     * {@code isMetaAnnotatedWith} rather than {@code isAnnotatedWith}, and no direct check beside
     * it: ArchUnit's meta variant "also matches elements that are directly annotated with the given
     * annotation type", so a plain {@code @Test} method is covered by the same call that reaches
     * {@code @ParameterizedTest} through {@code @TestTemplate} and a consumer's {@code @FastTest}
     * through {@code @Test}.
     */
    private static boolean isJUnitTestMethod(JavaMethod method) {
        return JUNIT_TEST_ANNOTATIONS.stream().anyMatch(method::isMetaAnnotatedWith);
    }

    static final ArchRule DEFINITION = classes()
            .that().containAnyMethodsThat(describe("annotated with a JUnit 5 test annotation",
                    TestClassNamingConventionRule::isJUnitTestMethod))
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
