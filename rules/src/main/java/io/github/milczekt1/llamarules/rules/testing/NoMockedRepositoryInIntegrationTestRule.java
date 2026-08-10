package io.github.milczekt1.llamarules.rules.testing;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.github.milczekt1.llamarules.FrozenRules;
import io.github.milczekt1.llamarules.RuleDoc;
import java.util.List;

/**
 * Integration tests must not mock repositories or daos.
 *
 * <p>Inspects <em>test</em> classes, so consumers must not set
 * {@code ImportOption.DoNotIncludeTests} — it would pass vacuously.
 */
public final class NoMockedRepositoryInIntegrationTestRule {

    /** Matched by FQN string, so a consumer missing any of these libraries still works. */
    static final List<String> FORBIDDEN_MOCK_ANNOTATIONS = List.of(
            "org.mockito.Mock",
            "org.springframework.test.context.bean.override.mockito.MockitoBean",
            // Removed in Spring Boot 4; retained for consumers still on Boot 3.
            "org.springframework.boot.test.mock.mockito.MockBean");

    static final RuleDoc DOC = RuleDoc.builder()
            .id("test.no-mocked-repository-in-integration-test")
            .why("""
                    An integration test exists to prove the real wiring works — schema, queries, mapping \
                    and transactions included. Mocking the repository or dao removes exactly the layer the \
                    test was written to exercise, leaving a slow test that proves nothing.""")
            .howToFix("""
                    Let the integration test hit the real persistence layer against a real database \
                    (Testcontainers or an equivalent). If you genuinely want to mock the persistence layer, \
                    the test is a unit test — rename it so it is no longer an integration test and move it \
                    beside the class it tests.""")
            .howNotToFix("""
                    Do NOT rename the class from FooIT to FooTests just to stop this rule matching while \
                    it keeps doing integration work, and do NOT rename the mocked type so it no longer ends \
                    in Repository or Dao. Both dodge the matcher and leave the problem in place.""")
            .build();

    static final ArchRule RULE = noClasses()
            .that().haveSimpleNameEndingWith("IT")
            .should(declareAMockedRepositoryOrDaoField());

    @ArchTest
    public static final ArchRule rule = FrozenRules.freeze(RULE, DOC);

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

    private NoMockedRepositoryInIntegrationTestRule() {
    }
}
