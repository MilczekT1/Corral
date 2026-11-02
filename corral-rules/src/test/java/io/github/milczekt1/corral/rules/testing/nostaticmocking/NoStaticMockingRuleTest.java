package io.github.milczekt1.corral.rules.testing.nostaticmocking;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.testing.nostaticmocking.fixtures.PlainMockitoUser;
import io.github.milczekt1.corral.rules.testing.nostaticmocking.fixtures.StaticHandleHolder;
import io.github.milczekt1.corral.rules.testing.nostaticmocking.fixtures.StaticHandleReceiver;
import io.github.milczekt1.corral.rules.testing.nostaticmocking.fixtures.StaticHandoffCaller;
import io.github.milczekt1.corral.rules.testing.nostaticmocking.fixtures.StaticMockingCaller;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import io.github.milczekt1.corral.scope.TestScope;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

/**
 * The examples live in {@code fixtures/}, which Surefire and Sonar both exclude: they exist to be
 * flagged, and a linter told to tidy any of them would delete the violation under test.
 */
class NoStaticMockingRuleTest {

    private static final String ID = "corral.test.mockito.no-static-mocking";

    /**
     * {@code Mockito} itself installs static mocks and is loaded from a jar, so it is
     * production-scoped: it pins the scope clause.
     */
    private static final JavaClasses EXAMPLES = new ClassFileImporter().importClasses(
            StaticMockingCaller.class, StaticHandleHolder.class, StaticHandleReceiver.class,
            StaticHandoffCaller.class, PlainMockitoUser.class, Mockito.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static String report() {
        return String.join("\n", NoStaticMockingRule.DEFINITION
                .allowEmptyShould(true).evaluate(EXAMPLES).getFailureReport().getDetails());
    }

    static Stream<Arguments> flags() {
        return Stream.of(
                arguments("a mockStatic call", "StaticMockingCaller"),
                arguments("the handle held as a field", "StaticHandleHolder"),
                arguments("the handle taken as a parameter", "StaticHandleReceiver"),
                arguments("a handle handed straight off", "StaticHandoffCaller"));
    }

    @ParameterizedTest(name = "flags {0}")
    @MethodSource
    void flags(String description, String offender) {
        String report = report();

        assertTrue(report.contains(offender), () -> "must flag " + description + ": " + report);
    }

    @Test
    void ignoresOrdinaryMockito() {
        String report = report();

        assertFalse(report.contains("PlainMockitoUser"), report);
    }

    /** The premise is a class both clauses would match, excluded only by scope. */
    @Test
    void scopesItselfToTestClasses() {
        JavaClass mockito = EXAMPLES.get(Mockito.class);

        assertFalse(TestScope.TEST_CLASSES.test(mockito),
                "premise: Mockito is loaded from a jar, so it has no test-output source");
        assertTrue(mockito.getDirectDependenciesFromSelf().stream()
                        .anyMatch(d -> NoStaticMockingRule.MOCK_HANDLE_TYPES
                                .contains(d.getTargetClass().getFullName())),
                "premise: Mockito must still depend on the handle type, or this pins nothing");

        assertFalse(report().contains("Method <org.mockito.Mockito."),
                () -> "Mockito appears only as a call target, never as an offender: " + report());

        String description = NoStaticMockingRule.DEFINITION.getDescription();
        assertTrue(description.contains("test classes"), description);
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoStaticMockingRule.rule, ID, EXAMPLES);

        assertTrue(debt.contains("StaticMockingCaller"), debt);
        assertTrue(debt.contains("StaticHandoffCaller"), debt);
    }
}
