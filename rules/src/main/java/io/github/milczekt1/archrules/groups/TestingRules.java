package io.github.milczekt1.archrules.groups;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.github.milczekt1.archrules.FrozenRules;
import io.github.milczekt1.archrules.RuleDoc;
import java.util.List;

/**
 * Rules about test hygiene.
 *
 * <p>Each rule exists twice: a package-private raw {@code *_RULE} constant for unit testing, plus a
 * public frozen {@code @ArchTest} field for consumers. Tests exercise the raw rules, because a
 * frozen rule seeds its violations and passes — which would make rule-correctness tests meaningless.
 *
 * <p>These rules inspect <em>test</em> classes, which is why consumers must not configure
 * {@code ImportOption.DoNotIncludeTests} — doing so makes them pass vacuously.
 */
public final class TestingRules {

    /** Matched by FQN string, so a consumer missing any of these libraries still works. */
    static final List<String> FORBIDDEN_MOCK_ANNOTATIONS = List.of(
            "org.mockito.Mock",
            "org.springframework.test.context.bean.override.mockito.MockitoBean",
            // Removed in Spring Boot 4; retained for consumers still on Boot 3.
            "org.springframework.boot.test.mock.mockito.MockBean");

    /**
     * Every JUnit 5 annotation that turns a method into an executable test, matched by FQN string
     * so a consumer without {@code junit-jupiter-params} still works.
     *
     * <p>All of them are listed explicitly because ArchUnit's {@code isAnnotatedWith} looks at
     * direct annotations only: {@code @ParameterizedTest} and {@code @RepeatedTest} are
     * meta-annotated with {@code @TestTemplate}, but that is invisible here. Missing one is a
     * false negative of exactly the kind this rule exists to catch — a class whose tests all use
     * {@code @ParameterizedTest} is just as silently unexecuted as one using {@code @Test}.
     */
    static final List<String> JUNIT_TEST_ANNOTATIONS = List.of(
            "org.junit.jupiter.api.Test",
            "org.junit.jupiter.api.RepeatedTest",
            "org.junit.jupiter.api.TestFactory",
            "org.junit.jupiter.api.TestTemplate",
            "org.junit.jupiter.params.ParameterizedTest");

    private TestingRules() {
    }

    // ------------------------------------------- integration tests must not mock repositories/daos

    static final RuleDoc NO_MOCKED_REPOS_IN_IT_DOC = RuleDoc.builder()
            .id("test.no-mocked-repository-in-integration-test")
            .why("An integration test exists to prove the real wiring works — schema, queries, mapping "
                    + "and transactions included. Mocking the repository or dao removes exactly the layer the "
                    + "test was written to exercise, leaving a slow test that proves nothing.")
            .howToFix("Let the integration test hit the real persistence layer against a real database "
                    + "(Testcontainers or an equivalent). If you genuinely want to mock the persistence layer, "
                    + "the test is a unit test — rename it so it is no longer an integration test and move it "
                    + "beside the class it tests.")
            .howNotToFix("Do NOT rename the class from FooIT to FooTests just to stop this rule matching while "
                    + "it keeps doing integration work, and do NOT rename the mocked type so it no longer ends "
                    + "in Repository or Dao. Both dodge the matcher and leave the problem in place.")
            .build();

    static final ArchRule NO_MOCKED_REPOS_IN_IT_RULE = noClasses()
            .that().haveSimpleNameEndingWith("IntegrationTest")
            .or().haveSimpleNameEndingWith("IT")
            .should(declareAMockedRepositoryOrDaoField());

    @ArchTest
    public static final ArchRule integrationTestsMustNotMockRepositoriesOrDaos =
            FrozenRules.freeze(NO_MOCKED_REPOS_IN_IT_RULE, NO_MOCKED_REPOS_IN_IT_DOC);

    /**
     * A field violates only when it is <em>both</em> annotated with a mocking annotation and typed
     * as a persistence abstraction. Used with {@code noClasses().should(...)}, so a satisfied event
     * is reported as a violation.
     */
    private static ArchCondition<JavaClass> declareAMockedRepositoryOrDaoField() {
        return new ArchCondition<>("declare a mocked Repository or Dao field") {
            @Override
            public void check(JavaClass testClass, ConditionEvents events) {
                for (JavaField field : testClass.getFields()) {
                    boolean mocked = FORBIDDEN_MOCK_ANNOTATIONS.stream().anyMatch(field::isAnnotatedWith);
                    String typeName = field.getRawType().getSimpleName();
                    boolean persistenceType = typeName.endsWith("Repository") || typeName.endsWith("Dao");
                    if (mocked && persistenceType) {
                        events.add(SimpleConditionEvent.satisfied(field,
                                "Field " + field.getFullName() + " mocks persistence type " + typeName));
                    }
                }
            }
        };
    }

    // ------------------------------------------------------------------- test class naming convention

    static final RuleDoc TEST_NAMING_DOC = RuleDoc.builder()
            .id("test.class-naming-convention")
            .why("Most build tools select which top-level classes to run by class-name convention (Maven's "
                    + "Surefire and Failsafe plugins, for example, match *Test and *IT respectively). A "
                    + "top-level class holding JUnit test methods — @Test, @ParameterizedTest, @RepeatedTest, "
                    + "@TestFactory or @TestTemplate — whose name ends in neither Test nor IT is silently "
                    + "never executed: it looks like coverage in the source tree while proving nothing in CI.")
            .howToFix("Rename the reported top-level class to end in Test (unit tests) or IT (integration "
                    + "tests) so your build tool's test-selection convention picks it up (with Maven, Surefire "
                    + "runs *Test and Failsafe runs *IT). Nested classes are never reported by this rule and "
                    + "must not be renamed: a JUnit 5 @Nested group is executed through its enclosing class, "
                    + "whose name is the only one the build tool ever looks at.")
            .howNotToFix("Do NOT delete the test methods or the class to make this rule pass, and do NOT "
                    + "widen your build tool's test-include configuration instead of renaming (for example, "
                    + "Surefire's include patterns) — the convention is what makes the unit/integration split "
                    + "legible. Do NOT swap @Test for @ParameterizedTest or any other JUnit test annotation "
                    + "either; every one of them counts.")
            .build();

    /**
     * Nested classes are excluded because no build tool selects them by name — the enclosing class
     * is what gets selected, so a JUnit 5 {@code @Nested} group (imported by ArchUnit as its own
     * {@code JavaClass} named e.g. {@code WhenEmpty}) is a guaranteed false positive whose only
     * "fix" would be a rename that changes nothing. {@code areNotMemberClasses()} also drops static
     * nested test-holders, which are genuinely unexecuted; that trade is accepted, because a rule
     * that fires on every {@code @Nested} class a consumer writes is unusable.
     */
    static final ArchRule TEST_NAMING_RULE = classes()
            .that().containAnyMethodsThat(describe("annotated with a JUnit 5 test annotation",
                    (JavaMethod method) ->
                            JUNIT_TEST_ANNOTATIONS.stream().anyMatch(method::isAnnotatedWith)))
            .and().areNotMemberClasses()
            .should().haveSimpleNameEndingWith("Test")
            .orShould().haveSimpleNameEndingWith("IT");

    @ArchTest
    public static final ArchRule testClassNamingConvention =
            FrozenRules.freeze(TEST_NAMING_RULE, TEST_NAMING_DOC);
}
