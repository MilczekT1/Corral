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
 * <p>Like {@link DatabaseRules}, each rule exists as a package-private raw {@code *_RULE} constant
 * for unit testing plus a public frozen {@code @ArchTest} field for consumers.
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

    private static final String JUNIT_TEST = "org.junit.jupiter.api.Test";

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
            .why("Surefire and Failsafe select tests by class name. A class holding @Test methods whose "
                    + "name ends in neither Test nor IT is silently never executed — it looks like coverage "
                    + "in the source tree while proving nothing in CI.")
            .howToFix("Rename the class to end in Test (unit tests, run by Surefire) or IT (integration "
                    + "tests, run by Failsafe).")
            .howNotToFix("Do NOT delete the @Test methods or the class to make this rule pass, and do NOT "
                    + "widen the Surefire include patterns instead of renaming — the convention is what makes "
                    + "the unit/integration split legible.")
            .build();

    static final ArchRule TEST_NAMING_RULE = classes()
            .that().containAnyMethodsThat(
                    describe("annotated with @Test", (JavaMethod method) -> method.isAnnotatedWith(JUNIT_TEST)))
            .should().haveSimpleNameEndingWith("Test")
            .orShould().haveSimpleNameEndingWith("IT");

    @ArchTest
    public static final ArchRule testClassNamingConvention =
            FrozenRules.freeze(TEST_NAMING_RULE, TEST_NAMING_DOC);
}
